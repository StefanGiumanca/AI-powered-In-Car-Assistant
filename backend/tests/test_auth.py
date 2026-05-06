import asyncio
import unittest
from unittest.mock import patch

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.security import verify_password
from app.db.database import get_db
from app.db.models import User
from app.main import app
from tests.asgi_client import request_app


REGISTER_PAYLOAD = {
    "email": "demo@test.com",
    "password": "test1234",
    "full_name": "Demo User",
}


class AuthEndpointTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.engine = create_engine(
            "sqlite://",
            connect_args={"check_same_thread": False},
            poolclass=StaticPool,
        )
        cls.SessionLocal = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=cls.engine,
        )

        def override_get_db():
            db = cls.SessionLocal()
            try:
                yield db
            finally:
                db.close()

        app.dependency_overrides[get_db] = override_get_db

    @classmethod
    def tearDownClass(cls) -> None:
        app.dependency_overrides.pop(get_db, None)
        cls.engine.dispose()

    def setUp(self) -> None:
        User.__table__.drop(bind=self.engine, checkfirst=True)
        User.__table__.create(bind=self.engine)

    def test_register_success_hashes_password(self) -> None:
        status, body = asyncio.run(
            request_app("POST", "/auth/register", REGISTER_PAYLOAD)
        )

        self.assertEqual(status, 201)
        self.assertEqual(body["email"], "demo@test.com")
        self.assertEqual(body["full_name"], "Demo User")
        self.assertNotIn("password", body)
        self.assertNotIn("password_hash", body)

        db = self.SessionLocal()
        try:
            user = db.query(User).filter(User.email == "demo@test.com").one()
            self.assertNotEqual(user.password_hash, "test1234")
            self.assertTrue(verify_password("test1234", user.password_hash))
        finally:
            db.close()

    def test_register_duplicate_email_returns_409(self) -> None:
        asyncio.run(request_app("POST", "/auth/register", REGISTER_PAYLOAD))

        status, body = asyncio.run(
            request_app("POST", "/auth/register", REGISTER_PAYLOAD)
        )

        self.assertEqual(status, 409)
        self.assertEqual(body["detail"], "Email already exists.")

    def test_login_success_returns_bearer_token(self) -> None:
        asyncio.run(request_app("POST", "/auth/register", REGISTER_PAYLOAD))

        status, body = asyncio.run(
            request_app(
                "POST",
                "/auth/login",
                {
                    "email": "demo@test.com",
                    "password": "test1234",
                },
            )
        )

        self.assertEqual(status, 200)
        self.assertEqual(body["token_type"], "bearer")
        self.assertTrue(body["access_token"])
        self.assertEqual(body["user"]["email"], "demo@test.com")

    def test_login_wrong_password_returns_401(self) -> None:
        asyncio.run(request_app("POST", "/auth/register", REGISTER_PAYLOAD))

        status, body = asyncio.run(
            request_app(
                "POST",
                "/auth/login",
                {
                    "email": "demo@test.com",
                    "password": "wrong-password",
                },
            )
        )

        self.assertEqual(status, 401)
        self.assertEqual(body["detail"], "Incorrect email or password.")

    def test_google_login_creates_user_and_returns_bearer_token(self) -> None:
        token_info = {
            "sub": "google-user-123",
            "email": "google@test.com",
            "email_verified": True,
            "name": "Google User",
        }

        with patch(
            "app.services.auth_service.verify_google_id_token",
            return_value=token_info,
        ):
            status, body = asyncio.run(
                request_app("POST", "/auth/google", {"id_token": "valid-token"})
            )

        self.assertEqual(status, 200)
        self.assertEqual(body["token_type"], "bearer")
        self.assertTrue(body["access_token"])
        self.assertEqual(body["user"]["email"], "google@test.com")
        self.assertEqual(body["user"]["full_name"], "Google User")

        db = self.SessionLocal()
        try:
            user = db.query(User).filter(User.email == "google@test.com").one()
            self.assertIsNone(user.password_hash)
            self.assertEqual(user.oauth_provider, "google")
            self.assertEqual(user.oauth_subject, "google-user-123")
        finally:
            db.close()

    def test_google_login_links_existing_email_user(self) -> None:
        asyncio.run(request_app("POST", "/auth/register", REGISTER_PAYLOAD))
        token_info = {
            "sub": "google-user-456",
            "email": "demo@test.com",
            "email_verified": True,
            "name": "Demo User",
        }

        with patch(
            "app.services.auth_service.verify_google_id_token",
            return_value=token_info,
        ):
            status, body = asyncio.run(
                request_app("POST", "/auth/google", {"id_token": "valid-token"})
            )

        self.assertEqual(status, 200)
        self.assertEqual(body["user"]["email"], "demo@test.com")

        db = self.SessionLocal()
        try:
            user = db.query(User).filter(User.email == "demo@test.com").one()
            self.assertEqual(user.oauth_provider, "google")
            self.assertEqual(user.oauth_subject, "google-user-456")
            self.assertTrue(verify_password("test1234", user.password_hash))
        finally:
            db.close()

    def test_google_login_invalid_token_returns_401(self) -> None:
        from fastapi import HTTPException, status

        with patch(
            "app.services.auth_service.verify_google_id_token",
            side_effect=HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid Google token.",
            ),
        ):
            status_code, body = asyncio.run(
                request_app("POST", "/auth/google", {"id_token": "invalid-token"})
            )

        self.assertEqual(status_code, 401)
        self.assertEqual(body["detail"], "Invalid Google token.")

    def test_me_success_returns_current_user(self) -> None:
        asyncio.run(request_app("POST", "/auth/register", REGISTER_PAYLOAD))
        _, login_body = asyncio.run(
            request_app(
                "POST",
                "/auth/login",
                {
                    "email": "demo@test.com",
                    "password": "test1234",
                },
            )
        )

        status, body = asyncio.run(
            request_app(
                "GET",
                "/auth/me",
                headers={
                    "Authorization": f"Bearer {login_body['access_token']}",
                },
            )
        )

        self.assertEqual(status, 200)
        self.assertEqual(body["email"], "demo@test.com")
        self.assertEqual(body["full_name"], "Demo User")

    def test_me_without_token_returns_401(self) -> None:
        status, body = asyncio.run(request_app("GET", "/auth/me"))

        self.assertEqual(status, 401)
        self.assertEqual(body["detail"], "Not authenticated.")


if __name__ == "__main__":
    unittest.main()

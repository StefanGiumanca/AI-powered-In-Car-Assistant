import uuid
from typing import Any

from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import create_access_token, hash_password, verify_password
from app.db.models import User
from app.schemas import (
    AuthUserResponse,
    GoogleLoginRequest,
    LoginRequest,
    LoginResponse,
    RegisterRequest,
)


def user_to_response(user: User) -> AuthUserResponse:
    return AuthUserResponse(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
    )


def get_user_by_email(db: Session, email: str) -> User | None:
    return db.query(User).filter(User.email == email).first()


def get_user_by_oauth_subject(
    db: Session,
    provider: str,
    subject: str,
) -> User | None:
    return (
        db.query(User)
        .filter(
            User.oauth_provider == provider,
            User.oauth_subject == subject,
        )
        .first()
    )


def get_user_by_id(db: Session, user_id: str) -> User | None:
    try:
        parsed_user_id = uuid.UUID(user_id)
    except ValueError:
        return None

    return db.query(User).filter(User.id == parsed_user_id).first()


def register_user(db: Session, payload: RegisterRequest) -> AuthUserResponse:
    existing_user = get_user_by_email(db, payload.email)
    if existing_user is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Email already exists.",
        )

    user = User(
        email=payload.email,
        password_hash=hash_password(payload.password),
        full_name=payload.full_name,
    )

    db.add(user)

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Email already exists.",
        ) from None

    db.refresh(user)
    return user_to_response(user)


def login_user(db: Session, payload: LoginRequest) -> LoginResponse:
    user = get_user_by_email(db, payload.email)

    if (
        user is None
        or user.password_hash is None
        or not verify_password(payload.password, user.password_hash)
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    access_token = create_access_token(user_id=str(user.id))

    return LoginResponse(
        access_token=access_token,
        token_type="bearer",
        user=user_to_response(user),
    )


def verify_google_id_token(id_token: str) -> dict[str, Any]:
    if not settings.google_oauth_client_id:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Google OAuth is not configured.",
        )

    try:
        from google.auth.transport import requests
        from google.oauth2 import id_token as google_id_token
    except ImportError:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Google OAuth dependencies are not installed.",
        ) from None

    try:
        token_info = google_id_token.verify_oauth2_token(
            id_token,
            requests.Request(),
            settings.google_oauth_client_id,
        )
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Google token.",
            headers={"WWW-Authenticate": "Bearer"},
        ) from None

    issuer = token_info.get("iss")
    if issuer not in {"accounts.google.com", "https://accounts.google.com"}:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Google token issuer.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    email = token_info.get("email")
    subject = token_info.get("sub")
    email_verified = token_info.get("email_verified")
    if not isinstance(email, str) or not isinstance(subject, str) or not email_verified:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Google account email is not verified.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return token_info


def login_google_user(db: Session, payload: GoogleLoginRequest) -> LoginResponse:
    token_info = verify_google_id_token(payload.id_token)
    email = token_info["email"].lower()
    subject = token_info["sub"]
    full_name = token_info.get("name") or email.split("@", maxsplit=1)[0]

    user = get_user_by_oauth_subject(db, "google", subject)
    if user is None:
        user = get_user_by_email(db, email)

    if user is None:
        user = User(
            email=email,
            password_hash=None,
            full_name=full_name,
            oauth_provider="google",
            oauth_subject=subject,
        )
        db.add(user)
    else:
        if user.oauth_subject is not None and user.oauth_subject != subject:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Email is already linked to another OAuth account.",
            )

        user.oauth_provider = "google"
        user.oauth_subject = subject
        if not user.full_name:
            user.full_name = full_name

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Google account is already linked to another user.",
        ) from None

    db.refresh(user)
    access_token = create_access_token(user_id=str(user.id))

    return LoginResponse(
        access_token=access_token,
        token_type="bearer",
        user=user_to_response(user),
    )

import uuid

from fastapi import HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import create_access_token, hash_password, verify_password
from app.db.models import User
from app.schemas import AuthUserResponse, LoginRequest, LoginResponse, RegisterRequest


def user_to_response(user: User) -> AuthUserResponse:
    return AuthUserResponse(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
    )


def get_user_by_email(db: Session, email: str) -> User | None:
    return db.query(User).filter(User.email == email).first()


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

    if user is None or not verify_password(payload.password, user.password_hash):
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

from typing import Annotated

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.db.models import User
from app.schemas import (
    AuthUserResponse,
    GoogleLoginRequest,
    LoginRequest,
    LoginResponse,
    RegisterRequest,
)
from app.services.auth_service import (
    login_google_user,
    login_user,
    register_user,
    user_to_response,
)


router = APIRouter(prefix="/auth", tags=["auth"])


@router.post(
    "/register",
    response_model=AuthUserResponse,
    status_code=status.HTTP_201_CREATED,
)
def register(
    payload: RegisterRequest,
    db: Annotated[Session, Depends(get_db)],
) -> AuthUserResponse:
    return register_user(db, payload)


@router.post("/login", response_model=LoginResponse)
def login(
    payload: LoginRequest,
    db: Annotated[Session, Depends(get_db)],
) -> LoginResponse:
    return login_user(db, payload)


@router.post("/google", response_model=LoginResponse)
def google_login(
    payload: GoogleLoginRequest,
    db: Annotated[Session, Depends(get_db)],
) -> LoginResponse:
    return login_google_user(db, payload)


@router.get("/me", response_model=AuthUserResponse)
def me(
    current_user: Annotated[User, Depends(get_current_user)],
) -> AuthUserResponse:
    return user_to_response(current_user)

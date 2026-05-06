from typing import Literal

from pydantic import Field, field_validator

from .common import APIModel


# DTOs for authentication requests and responses.
class AuthUserResponse(APIModel):
    id: str = Field(..., min_length=1)
    email: str = Field(..., min_length=3, max_length=255)
    full_name: str | None = Field(default=None, max_length=255)


class RegisterRequest(APIModel):
    email: str = Field(
        ...,
        min_length=3,
        max_length=255,
        pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$",
    )
    password: str = Field(..., min_length=8, max_length=128)
    full_name: str = Field(..., min_length=1, max_length=255)

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        return value.lower()


class LoginRequest(APIModel):
    email: str = Field(
        ...,
        min_length=3,
        max_length=255,
        pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$",
    )
    password: str = Field(..., min_length=1, max_length=128)

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        return value.lower()


class GoogleLoginRequest(APIModel):
    id_token: str = Field(..., min_length=1)


class LoginResponse(APIModel):
    access_token: str = Field(..., min_length=1)
    token_type: Literal["bearer"] = "bearer"
    user: AuthUserResponse

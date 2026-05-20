from typing import Literal

from pydantic import Field

from .common import APIModel


ProfileType = Literal[
    "fastest",
    "cheapest",
    "family",
    "scenic",
    "balanced",
    "business",
]


class DriverProfileCreateRequest(APIModel):
    profile_name: str = Field(..., min_length=1, max_length=100)
    profile_type: ProfileType = "balanced"

    preferred_amenities: list[str] = Field(default_factory=list)
    preferred_brands: list[str] = Field(default_factory=list)

    avoid_tolls: bool = False
    avoid_highways: bool = False
    max_detour_minutes: int = Field(default=10, ge=0)
    break_frequency_minutes: int = Field(default=120, ge=0)


class DriverProfileResponse(APIModel):
    id: str
    profile_name: str | None = None
    profile_type: ProfileType

    preferred_amenities: list[str]
    preferred_brands: list[str]

    avoid_tolls: bool
    avoid_highways: bool
    max_detour_minutes: int
    break_frequency_minutes: int

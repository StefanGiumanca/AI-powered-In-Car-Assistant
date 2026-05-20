from typing import Literal

from pydantic import Field

from .common import APIModel
from .vehicles import VehicleDTO


# DTOs for the current user, driver profile, and app bootstrap response.
class UserDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    full_name: str = Field(..., min_length=1, max_length=160)


class DriverProfileDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    profile_type: Literal[
    "fastest",
    "cheapest",
    "family",
    "scenic",
    "balanced",
    "business",
]
    avoid_tolls: bool
    max_detour_minutes: int = Field(..., ge=0)


class BootstrapResponse(APIModel):
    user: UserDTO
    vehicle: VehicleDTO
    driver_profile: DriverProfileDTO

from datetime import datetime
from typing import Literal

from pydantic import Field

from .common import APIModel


class BootstrapUserDTO(APIModel):
    id: str = Field(..., min_length=1)
    email: str = Field(..., min_length=1)
    full_name: str | None = None


class BootstrapVehicleDTO(APIModel):
    id: str = Field(..., min_length=1)
    model: str | None = None
    year: int | None = None
    powertrain: Literal["EV", "ICE", "HYBRID"]

    fuel_tank_liters: float | None = None
    consumption_l_per_100km: float | None = None

    battery_capacity_kwh: float | None = None
    consumption_kwh_per_100km: float | None = None
    connector_type: str | None = None


class BootstrapDriverProfileDTO(APIModel):
    id: str = Field(..., min_length=1)
    profile_name: str | None = None
    profile_type: Literal[
        "fastest",
        "cheapest",
        "family",
        "scenic",
        "balanced",
        "business",
    ]

    avoid_tolls: bool
    avoid_highways: bool
    max_detour_minutes: int
    break_frequency_minutes: int

    preferred_brands: list[str]
    preferred_amenities: list[str]


class BootstrapVehicleStateDTO(APIModel):
    id: str = Field(..., min_length=1)
    vehicle_id: str = Field(..., min_length=1)

    fuel_level_percent: float | None = None
    battery_soc_percent: float | None = None
    estimated_range_km: float | None = None
    odometer_km: float | None = None
    tire_pressure_status: str | None = None

    captured_at: datetime


class BootstrapResponse(APIModel):
    user: BootstrapUserDTO
    vehicles: list[BootstrapVehicleDTO]
    driver_profiles: list[BootstrapDriverProfileDTO]
    latest_vehicle_state: BootstrapVehicleStateDTO | None

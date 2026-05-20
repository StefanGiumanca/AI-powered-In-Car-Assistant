from datetime import datetime
from typing import Literal

from pydantic import Field

from .common import APIModel


VehicleStateTirePressureStatus = Literal["ok", "low", "warning", "unknown"]


class VehicleStateCreateRequest(APIModel):
    vehicle_id: str = Field(..., min_length=1)

    fuel_level_percent: float | None = Field(default=None, ge=0, le=100)
    battery_soc_percent: float | None = Field(default=None, ge=0, le=100)

    estimated_range_km: float | None = Field(default=None, ge=0)
    odometer_km: float | None = Field(default=None, ge=0)

    tire_pressure_status: VehicleStateTirePressureStatus = "unknown"


class VehicleStateResponse(APIModel):
    id: str
    vehicle_id: str

    fuel_level_percent: float | None = None
    battery_soc_percent: float | None = None
    estimated_range_km: float | None = None
    odometer_km: float | None = None

    tire_pressure_status: VehicleStateTirePressureStatus | None = None
    captured_at: datetime

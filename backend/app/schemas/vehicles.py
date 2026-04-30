from typing import Literal

from pydantic import Field, model_validator

from .common import APIModel, TirePressureStatus


# DTOs for vehicle details and vehicle state snapshots sent by the mobile app.
class VehicleDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    model: str = Field(..., min_length=1, max_length=120)
    powertrain: Literal["ICE", "EV", "HYBRID"]

    fuel_tank_liters: float | None = Field(default=None, gt=0)
    consumption_l_per_100km: float | None = Field(default=None, gt=0)

    battery_capacity_kwh: float | None = Field(default=None, gt=0)
    consumption_kwh_per_100km: float | None = Field(default=None, gt=0)
    connector_type: str | None = None


class VehicleStateDTO(APIModel):
    fuel_level_percent: float | None = Field(default=None, ge=0, le=100)
    battery_soc_percent: float | None = Field(default=None, ge=0, le=100)
    estimated_range_km: float = Field(..., ge=0)
    odometer_km: float = Field(..., ge=0)
    tire_pressure_status: TirePressureStatus
    engine_warning: bool | None = None

    @model_validator(mode="after")
    def has_energy_level(self) -> "VehicleStateDTO":
        if self.fuel_level_percent is None and self.battery_soc_percent is None:
            raise ValueError(
                "Either fuel_level_percent or battery_soc_percent must be provided."
            )
        return self

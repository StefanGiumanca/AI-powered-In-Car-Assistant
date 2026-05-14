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


class VehicleCreateRequest(APIModel):
    vin: str | None = Field(default=None, max_length=50)
    model: str = Field(..., min_length=1, max_length=100)
    year: int | None = Field(default=None, ge=1900, le=2100)

    powertrain: Literal["EV", "ICE", "HYBRID"]

    connector_type: str | None = Field(default=None, max_length=50)
    battery_capacity_kwh: float | None = Field(default=None, gt=0)
    fuel_tank_liters: float | None = Field(default=None, gt=0)

    consumption_kwh_per_100km: float | None = Field(default=None, gt=0)
    consumption_l_per_100km: float | None = Field(default=None, gt=0)

    service_interval_km: int | None = Field(default=None, gt=0)
    service_interval_months: int | None = Field(default=None, gt=0)

    @model_validator(mode="after")
    def validate_powertrain_fields(self) -> "VehicleCreateRequest":
        if self.powertrain == "EV" and self.fuel_tank_liters is not None:
            raise ValueError("EV vehicles should not include fuel_tank_liters.")

        if self.powertrain == "ICE" and self.battery_capacity_kwh is not None:
            raise ValueError("ICE vehicles should not include battery_capacity_kwh.")

        return self


class VehicleUpdateRequest(VehicleCreateRequest):
    pass


class VehicleResponse(APIModel):
    id: str
    vin: str | None = None
    model: str
    year: int | None = None
    powertrain: Literal["EV", "ICE", "HYBRID"]

    connector_type: str | None = None
    battery_capacity_kwh: float | None = None
    fuel_tank_liters: float | None = None

    consumption_kwh_per_100km: float | None = None
    consumption_l_per_100km: float | None = None

    service_interval_km: int | None = None
    service_interval_months: int | None = None


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

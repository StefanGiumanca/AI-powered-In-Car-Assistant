from pydantic import Field

from .common import (
    APIModel,
    CurrentLocationDTO,
    LabeledLocationDTO,
    RequestedMode,
    TripStatus,
)
from .routes import RouteDTO
from .vehicle_state import VehicleStateTirePressureStatus


class TripStartRequest(APIModel):
    vehicle_id: str = Field(..., min_length=1, max_length=100)
    driver_profile_id: str = Field(..., min_length=1, max_length=100)
    origin: LabeledLocationDTO
    destination: LabeledLocationDTO
    requested_mode: RequestedMode


class TripDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    status: TripStatus
    origin: LabeledLocationDTO
    destination: LabeledLocationDTO
    requested_mode: RequestedMode


class TripStartResponse(APIModel):
    trip: TripDTO
    route: RouteDTO


class TripDetailResponse(TripStartResponse):
    pass


class TripUpdateVehicleStateDTO(APIModel):
    fuel_level_percent: float | None = Field(default=None, ge=0, le=100)
    battery_soc_percent: float | None = Field(default=None, ge=0, le=100)
    estimated_range_km: float | None = Field(default=None, ge=0)
    odometer_km: float | None = Field(default=None, ge=0)
    tire_pressure_status: VehicleStateTirePressureStatus = "unknown"


class TripUpdateRequest(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)
    current_location: CurrentLocationDTO
    vehicle_state: TripUpdateVehicleStateDTO


class TripUpdateResponse(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)
    status: TripStatus
    should_refresh_recommendation: bool

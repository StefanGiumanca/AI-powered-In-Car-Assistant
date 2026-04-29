from pydantic import Field

from .common import (
    APIModel,
    CurrentLocationDTO,
    LabeledLocationDTO,
    RequestedMode,
    TripStatus,
)
from .recommendations import RecommendationDTO
from .vehicles import VehicleStateDTO


# DTOs for starting and updating trips, including selected route and recommendation data.
class RouteDTO(APIModel):
    route_id: str = Field(..., min_length=1, max_length=100)
    distance_km: float = Field(..., ge=0)
    duration_minutes: int = Field(..., ge=0)
    traffic_duration_minutes: int = Field(..., ge=0)
    estimated_fuel_liters: float | None = Field(..., ge=0)
    polyline: str | None = Field(...)


class TripStartRequest(APIModel):
    user_id: str = Field(..., min_length=1, max_length=100)
    vehicle_id: str = Field(..., min_length=1, max_length=100)
    driver_profile_id: str = Field(..., min_length=1, max_length=100)
    origin: LabeledLocationDTO
    destination: LabeledLocationDTO
    vehicle_state: VehicleStateDTO
    requested_mode: RequestedMode


class TripStartResponse(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)
    status: TripStatus
    selected_route: RouteDTO
    next_recommendation: RecommendationDTO


class TripUpdateRequest(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)
    current_location: CurrentLocationDTO
    vehicle_state: VehicleStateDTO


class TripUpdateResponse(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)
    status: TripStatus
    should_refresh_recommendation: bool

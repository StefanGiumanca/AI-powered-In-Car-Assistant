from typing import Literal

from pydantic import Field

from .common import (
    APIModel,
    CurrentLocationDTO,
    LabeledLocationDTO,
    RequestedMode,
    TripStatus,
)
from .vehicle_state import VehicleStateTirePressureStatus


# DTOs for the authenticated MVP trip flow and its mock route/recommendation data.
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


class RouteDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    provider: str = Field(..., min_length=1, max_length=50)
    route_name: str = Field(..., min_length=1, max_length=255)
    distance_km: float = Field(..., ge=0)
    duration_minutes: int = Field(..., ge=0)
    traffic_duration_minutes: int = Field(..., ge=0)
    estimated_fuel_liters: float | None = Field(default=None, ge=0)
    estimated_energy_kwh: float | None = Field(default=None, ge=0)
    toll_cost: float = Field(..., ge=0)
    route_score: float = Field(..., ge=0, le=1)
    is_selected: bool


class TripRecommendationDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    action: Literal[
        "STOP_FOR_FUEL",
        "STOP_FOR_CHARGING",
        "SERVICE_CHECK",
        "CONTINUE_TRIP",
    ]
    type: Literal["route", "stop", "service", "itinerary"]
    item_type: Literal[
        "route",
        "charging_stop",
        "fuel_stop",
        "food_stop",
        "hotel_stop",
        "service_center",
        "rest_stop",
        "parking",
    ]
    title: str = Field(..., min_length=1, max_length=255)
    reason: str = Field(..., min_length=1)
    priority: Literal["low", "medium", "high"]


class TripStartResponse(APIModel):
    trip: TripDTO
    selected_route: RouteDTO
    recommendation: TripRecommendationDTO


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

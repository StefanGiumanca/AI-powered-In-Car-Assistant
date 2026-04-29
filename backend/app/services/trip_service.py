from fastapi import HTTPException, status

from app.schemas import (
    RouteDTO,
    TripStartRequest,
    TripStartResponse,
    TripUpdateRequest,
    TripUpdateResponse,
    VehicleStateDTO,
)
from app.services import mock_state
from app.services.recommendation_service import build_next_action, build_recommendation


def start_trip(payload: TripStartRequest) -> TripStartResponse:
    trip_id = mock_state.next_trip_id()
    recommendation_id = mock_state.next_recommendation_id()

    selected_route = RouteDTO(
        route_id="route_1",
        distance_km=168,
        duration_minutes=155,
        traffic_duration_minutes=172,
        estimated_fuel_liters=12.1,
        polyline=None,
    )
    recommendation = build_recommendation(
        recommendation_id=recommendation_id,
        vehicle_state=payload.vehicle_state,
    )
    next_action = build_next_action(
        recommendation_id=recommendation_id,
        vehicle_state=payload.vehicle_state,
    )

    mock_state.trips[trip_id] = {
        "trip_id": trip_id,
        "status": "active",
        "request": payload.model_dump(),
        "selected_route": selected_route.model_dump(),
    }
    mock_state.vehicle_state_snapshots.append(
        {
            "trip_id": trip_id,
            "source": "trip_start",
            "vehicle_state": payload.vehicle_state.model_dump(),
        }
    )
    mock_state.recommendations[recommendation_id] = recommendation
    mock_state.next_actions_by_trip_id[trip_id] = next_action

    return TripStartResponse(
        trip_id=trip_id,
        status="active",
        selected_route=selected_route,
        next_recommendation=recommendation,
    )


def update_trip(payload: TripUpdateRequest) -> TripUpdateResponse:
    trip = mock_state.trips.get(payload.trip_id)
    if trip is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trip not found.",
        )

    mock_state.vehicle_state_snapshots.append(
        {
            "trip_id": payload.trip_id,
            "source": "trip_update",
            "current_location": payload.current_location.model_dump(),
            "vehicle_state": payload.vehicle_state.model_dump(),
        }
    )

    recommendation_id = mock_state.next_recommendation_id()
    recommendation = build_recommendation(
        recommendation_id=recommendation_id,
        vehicle_state=payload.vehicle_state,
    )
    next_action = build_next_action(
        recommendation_id=recommendation_id,
        vehicle_state=payload.vehicle_state,
    )
    mock_state.recommendations[recommendation_id] = recommendation
    mock_state.next_actions_by_trip_id[payload.trip_id] = next_action

    return TripUpdateResponse(
        trip_id=payload.trip_id,
        status=trip["status"],
        should_refresh_recommendation=should_refresh_recommendation(
            payload.vehicle_state
        ),
    )


def should_refresh_recommendation(vehicle_state: VehicleStateDTO) -> bool:
    fuel_is_low = (
        vehicle_state.fuel_level_percent is not None
        and vehicle_state.fuel_level_percent < 20
    )
    tire_pressure_changed = vehicle_state.tire_pressure_status != "ok"

    return fuel_is_low or tire_pressure_changed or vehicle_state.engine_warning is True

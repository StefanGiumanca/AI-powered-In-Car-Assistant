from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas import (
    LabeledLocationDTO,
    RouteDTO,
    TripDTO,
    TripDetailResponse,
    TripStartRequest,
    TripStartResponse,
    TripUpdateRequest,
    TripUpdateResponse,
)
from app.services.routes_service import get_route


def to_uuid(value: str, field_name: str) -> UUID:
    try:
        return UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid UUID for {field_name}.",
        )


def start_trip(
    payload: TripStartRequest,
    current_user: dbm.User,
    db: Session,
) -> TripStartResponse:
    vehicle_id = to_uuid(payload.vehicle_id, "vehicle_id")
    driver_profile_id = to_uuid(payload.driver_profile_id, "driver_profile_id")

    vehicle = get_owned_vehicle(vehicle_id, current_user, db)
    driver_profile = get_owned_driver_profile(driver_profile_id, current_user, db)

    route_data = get_route(
        origin_lat=payload.origin.lat,
        origin_lng=payload.origin.lng,
        dest_lat=payload.destination.lat,
        dest_lng=payload.destination.lng,
        intermediates=[
            {
                "lat": stop.lat,
                "lng": stop.lng,
            }
            for stop in payload.stops
        ],
    )

    db_trip = dbm.Trip(
        user_id=current_user.id,
        vehicle_id=vehicle.id,
        driver_profile_id=driver_profile.id,
        origin_label=payload.origin.label,
        origin_lat=payload.origin.lat,
        origin_lng=payload.origin.lng,
        destination_label=payload.destination.label,
        destination_lat=payload.destination.lat,
        destination_lng=payload.destination.lng,
        requested_mode=payload.requested_mode,
        status="active",
    )

    db.add(db_trip)
    db.flush()

    selected_route = create_route_option_for_trip(db_trip, route_data, db)

    db.commit()
    db.refresh(db_trip)
    db.refresh(selected_route)

    return build_trip_response(db_trip, selected_route)


def get_trip(
    trip_id_value: str,
    current_user: dbm.User,
    db: Session,
) -> TripDetailResponse:
    trip_id = to_uuid(trip_id_value, "trip_id")
    trip = (
        db.query(dbm.Trip)
        .filter(dbm.Trip.id == trip_id, dbm.Trip.user_id == current_user.id)
        .first()
    )
    if trip is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Trip not found.")

    selected_route = (
        db.query(dbm.RouteOption)
        .filter(dbm.RouteOption.trip_id == trip.id, dbm.RouteOption.is_selected.is_(True))
        .first()
    )
    if selected_route is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Selected route not found.",
        )

    return build_trip_response(trip, selected_route)


def update_trip(
    payload: TripUpdateRequest,
    current_user: dbm.User,
    db: Session,
) -> TripUpdateResponse:
    trip_id = to_uuid(payload.trip_id, "trip_id")
    trip = (
        db.query(dbm.Trip)
        .filter(dbm.Trip.id == trip_id, dbm.Trip.user_id == current_user.id)
        .first()
    )
    if trip is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Trip not found.")

    if trip.status != "active":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Trip is not active.",
        )

    db_vehicle_state = dbm.VehicleStateSnapshot(
        vehicle_id=trip.vehicle_id,
        battery_soc_percent=payload.vehicle_state.battery_soc_percent,
        fuel_level_percent=payload.vehicle_state.fuel_level_percent,
        estimated_range_km=payload.vehicle_state.estimated_range_km,
        odometer_km=payload.vehicle_state.odometer_km,
        tire_pressure_status=payload.vehicle_state.tire_pressure_status,
    )

    db.add(db_vehicle_state)
    db.commit()

    return TripUpdateResponse(
        trip_id=str(trip.id),
        status=trip.status,
        should_refresh_recommendation=should_refresh_recommendation(payload.vehicle_state),
    )


def get_owned_vehicle(
    vehicle_id: UUID,
    current_user: dbm.User,
    db: Session,
) -> dbm.Vehicle:
    vehicle = (
        db.query(dbm.Vehicle)
        .filter(dbm.Vehicle.id == vehicle_id, dbm.Vehicle.user_id == current_user.id)
        .first()
    )
    if vehicle is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Vehicle not found.")
    return vehicle


def get_owned_driver_profile(
    driver_profile_id: UUID,
    current_user: dbm.User,
    db: Session,
) -> dbm.DriverProfile:
    driver_profile = (
        db.query(dbm.DriverProfile)
        .filter(
            dbm.DriverProfile.id == driver_profile_id,
            dbm.DriverProfile.user_id == current_user.id,
        )
        .first()
    )
    if driver_profile is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Driver profile not found.",
        )
    return driver_profile


def create_route_option_for_trip(
    trip: dbm.Trip,
    route_data: dict,
    db: Session,
) -> dbm.RouteOption:
    route = dbm.RouteOption(
        trip_id=trip.id,
        provider="google",
        route_name=None,
        polyline=route_data["polyline"],
        distance_km=route_data["distance_km"],
        duration_minutes=route_data["duration_minutes"],
        traffic_duration_minutes=None,
        estimated_energy_kwh=None,
        estimated_fuel_liters=None,
        estimated_cost=None,
        toll_cost=None,
        route_score=None,
        is_selected=True,
        scoring_breakdown=None,
    )

    db.add(route)
    db.flush()
    return route


def should_refresh_recommendation(vehicle_state) -> bool:
    fuel_is_low = (
        vehicle_state.fuel_level_percent is not None
        and vehicle_state.fuel_level_percent < 25
    )
    battery_is_low = (
        vehicle_state.battery_soc_percent is not None
        and vehicle_state.battery_soc_percent < 25
    )
    tire_pressure_needs_attention = vehicle_state.tire_pressure_status in {
        "low",
        "warning",
    }

    return fuel_is_low or battery_is_low or tire_pressure_needs_attention


def build_trip_response(
    trip: dbm.Trip,
    selected_route: dbm.RouteOption,
) -> TripStartResponse:
    return TripStartResponse(
        trip=TripDTO(
            id=str(trip.id),
            status=trip.status,
            origin=LabeledLocationDTO(
                label=trip.origin_label,
                lat=float(trip.origin_lat),
                lng=float(trip.origin_lng),
            ),
            destination=LabeledLocationDTO(
                label=trip.destination_label,
                lat=float(trip.destination_lat),
                lng=float(trip.destination_lng),
            ),
            stops=[],
            requested_mode=trip.requested_mode,
        ),
        route=RouteDTO(
            distance_km=float(selected_route.distance_km or 0),
            duration_minutes=int(selected_route.duration_minutes or 0),
            polyline=selected_route.polyline or "",
        ),
    )

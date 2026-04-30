from dataclasses import dataclass
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas import (
    LabeledLocationDTO,
    RouteDTO,
    TripDTO,
    TripDetailResponse,
    TripRecommendationDTO,
    TripStartRequest,
    TripStartResponse,
    TripUpdateRequest,
    TripUpdateResponse,
)


@dataclass(frozen=True)
class MockRecommendation:
    action: str
    recommendation_type: str
    item_type: str
    title: str
    reason: str
    priority: str


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
    vehicle_state = get_latest_vehicle_state(vehicle.id, db)
    if vehicle_state is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No vehicle state snapshot found for vehicle.",
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

    selected_route = create_mock_route_for_trip(db_trip, vehicle, db)
    recommendation = choose_mock_recommendation(vehicle_state)
    db_recommendation, db_item = create_mock_recommendation(
        db_trip,
        selected_route,
        recommendation,
        db,
    )

    db.commit()
    db.refresh(db_trip)
    db.refresh(selected_route)
    db.refresh(db_recommendation)
    db.refresh(db_item)

    return build_trip_response(db_trip, selected_route, db_recommendation, db_item)


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

    recommendation = (
        db.query(dbm.Recommendation)
        .filter(dbm.Recommendation.trip_id == trip.id)
        .order_by(dbm.Recommendation.created_at.desc())
        .first()
    )
    if recommendation is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Recommendation not found.",
        )

    item = (
        db.query(dbm.RecommendationItem)
        .filter(dbm.RecommendationItem.recommendation_id == recommendation.id)
        .order_by(dbm.RecommendationItem.rank_position.asc())
        .first()
    )
    if item is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Recommendation item not found.",
        )

    return build_trip_response(trip, selected_route, recommendation, item)


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


def get_latest_vehicle_state(
    vehicle_id: UUID,
    db: Session,
) -> dbm.VehicleStateSnapshot | None:
    return (
        db.query(dbm.VehicleStateSnapshot)
        .filter(dbm.VehicleStateSnapshot.vehicle_id == vehicle_id)
        .order_by(dbm.VehicleStateSnapshot.captured_at.desc())
        .first()
    )


def create_mock_route_for_trip(
    trip: dbm.Trip,
    vehicle: dbm.Vehicle,
    db: Session,
) -> dbm.RouteOption:
    distance_km = 168
    estimated_fuel_liters = None
    estimated_energy_kwh = None

    if vehicle.consumption_l_per_100km is not None:
        estimated_fuel_liters = round(
            distance_km * float(vehicle.consumption_l_per_100km) / 100,
            1,
        )

    if vehicle.consumption_kwh_per_100km is not None:
        estimated_energy_kwh = round(
            distance_km * float(vehicle.consumption_kwh_per_100km) / 100,
            1,
        )

    route = dbm.RouteOption(
        trip_id=trip.id,
        provider="mock",
        route_name="Mock fastest route",
        polyline=None,
        distance_km=distance_km,
        duration_minutes=155,
        traffic_duration_minutes=172,
        estimated_energy_kwh=estimated_energy_kwh,
        estimated_fuel_liters=estimated_fuel_liters,
        estimated_cost=None,
        toll_cost=0,
        route_score=0.8,
        is_selected=True,
        scoring_breakdown={"source": "mock_trip_flow"},
    )

    db.add(route)
    db.flush()
    return route


def choose_mock_recommendation(
    vehicle_state: dbm.VehicleStateSnapshot,
) -> MockRecommendation:
    if (
        vehicle_state.fuel_level_percent is not None
        and float(vehicle_state.fuel_level_percent) < 25
    ):
        return MockRecommendation(
            action="STOP_FOR_FUEL",
            recommendation_type="stop",
            item_type="fuel_stop",
            title="Stop for fuel soon",
            reason="Fuel level is low. Plan a fuel stop before continuing.",
            priority="high",
        )

    if (
        vehicle_state.battery_soc_percent is not None
        and float(vehicle_state.battery_soc_percent) < 25
    ):
        return MockRecommendation(
            action="STOP_FOR_CHARGING",
            recommendation_type="stop",
            item_type="charging_stop",
            title="Stop for charging soon",
            reason="Battery level is low. Plan a charging stop before continuing.",
            priority="high",
        )

    if vehicle_state.tire_pressure_status in {"low", "warning"}:
        return MockRecommendation(
            action="SERVICE_CHECK",
            recommendation_type="service",
            item_type="service_center",
            title="Check tire pressure",
            reason="Tire pressure warning was reported.",
            priority="medium",
        )

    return MockRecommendation(
        action="CONTINUE_TRIP",
        recommendation_type="route",
        item_type="route",
        title="Continue your trip",
        reason="Vehicle state looks good for this trip.",
        priority="low",
    )


def create_mock_recommendation(
    trip: dbm.Trip,
    selected_route: dbm.RouteOption,
    recommendation: MockRecommendation,
    db: Session,
) -> tuple[dbm.Recommendation, dbm.RecommendationItem]:
    db_recommendation = dbm.Recommendation(
        trip_id=trip.id,
        recommendation_type=recommendation.recommendation_type,
        selected_route_option_id=selected_route.id,
        final_score=0.8,
        explanation=recommendation.reason,
        scoring_breakdown={
            "source": "mock_trip_flow",
            "action": recommendation.action,
            "title": recommendation.title,
            "priority": recommendation.priority,
            "item_type": recommendation.item_type,
        },
    )

    db.add(db_recommendation)
    db.flush()

    db_item = dbm.RecommendationItem(
        recommendation_id=db_recommendation.id,
        route_option_id=selected_route.id,
        item_type=recommendation.item_type,
        rank_position=1,
        score=0.8,
        reason=recommendation.reason,
        accepted=None,
    )

    db.add(db_item)
    db.flush()
    return db_recommendation, db_item


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
    recommendation: dbm.Recommendation,
    item: dbm.RecommendationItem,
) -> TripStartResponse:
    metadata = recommendation.scoring_breakdown or {}

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
            requested_mode=trip.requested_mode,
        ),
        selected_route=RouteDTO(
            id=str(selected_route.id),
            provider=selected_route.provider,
            route_name=selected_route.route_name,
            distance_km=float(selected_route.distance_km or 0),
            duration_minutes=int(selected_route.duration_minutes or 0),
            traffic_duration_minutes=int(selected_route.traffic_duration_minutes or 0),
            estimated_fuel_liters=(
                float(selected_route.estimated_fuel_liters)
                if selected_route.estimated_fuel_liters is not None
                else None
            ),
            estimated_energy_kwh=(
                float(selected_route.estimated_energy_kwh)
                if selected_route.estimated_energy_kwh is not None
                else None
            ),
            toll_cost=float(selected_route.toll_cost or 0),
            route_score=float(selected_route.route_score or 0),
            is_selected=bool(selected_route.is_selected),
        ),
        recommendation=TripRecommendationDTO(
            id=str(recommendation.id),
            action=metadata.get("action", "CONTINUE_TRIP"),
            type=recommendation.recommendation_type,
            item_type=metadata.get("item_type", item.item_type),
            title=metadata.get("title", "Continue your trip"),
            reason=item.reason or recommendation.explanation,
            priority=metadata.get("priority", "low"),
        ),
    )

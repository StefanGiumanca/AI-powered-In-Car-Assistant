from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas import (
    RouteDTO,
    TripStartRequest,
    TripStartResponse,
    TripUpdateRequest,
    TripUpdateResponse,
    VehicleStateDTO,
)
from app.services.recommendation_service import (
    build_recommendation,
    build_next_action,
)


def to_uuid(value: str) -> UUID:
    try:
        return UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid UUID: {value}",
        )


def start_trip(payload: TripStartRequest, db: Session) -> TripStartResponse:
    user_id = to_uuid(payload.user_id)
    vehicle_id = to_uuid(payload.vehicle_id)
    driver_profile_id = to_uuid(payload.driver_profile_id)

    user = db.query(dbm.User).filter(dbm.User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found.")

    vehicle = db.query(dbm.Vehicle).filter(dbm.Vehicle.id == vehicle_id).first()
    if vehicle is None:
        raise HTTPException(status_code=404, detail="Vehicle not found.")

    driver_profile = (
        db.query(dbm.DriverProfile)
        .filter(dbm.DriverProfile.id == driver_profile_id)
        .first()
    )
    if driver_profile is None:
        raise HTTPException(status_code=404, detail="Driver profile not found.")

    db_trip = dbm.Trip(
        user_id=user_id,
        vehicle_id=vehicle_id,
        driver_profile_id=driver_profile_id,
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

    db_vehicle_state = dbm.VehicleStateSnapshot(
        vehicle_id=vehicle_id,
        battery_soc_percent=payload.vehicle_state.battery_soc_percent,
        fuel_level_percent=payload.vehicle_state.fuel_level_percent,
        estimated_range_km=payload.vehicle_state.estimated_range_km,
        odometer_km=payload.vehicle_state.odometer_km,
        tire_pressure_status=payload.vehicle_state.tire_pressure_status,
    )

    db.add(db_vehicle_state)
    db.flush()

    selected_route = create_mock_route_for_trip(db_trip, db)

    recommendation = build_recommendation(
        recommendation_id=None,
        vehicle_state=payload.vehicle_state,
    )

    db_recommendation = dbm.Recommendation(
        trip_id=db_trip.id,
        recommendation_type="itinerary",
        selected_route_option_id=selected_route.id,
        final_score=0.80,
        explanation=recommendation.reason,
        scoring_breakdown={
            "source": "temporary_rule_based_recommendation",
            "requested_mode": payload.requested_mode,
        },
    )

    db.add(db_recommendation)
    db.flush()

    location = get_or_create_recommendation_location(recommendation, db)

    db_item = dbm.RecommendationItem(
        recommendation_id=db_recommendation.id,
        route_option_id=selected_route.id,
        location_id=location.id,
        item_type=recommendation.type,
        rank_position=1,
        score=0.80,
        driver_value_score=0.75,
        partner_boost_score=0.03,
        loyalty_score=0.02,
        reason=recommendation.reason,
        accepted=None,
    )

    db.add(db_item)

    db_event = dbm.RecommendationEvent(
        user_id=user_id,
        trip_id=db_trip.id,
        recommendation_id=db_recommendation.id,
        recommendation_item_id=db_item.id,
        event_type="generated",
        event_metadata={
            "source": "trip_start",
            "recommendation_type": recommendation.type,
        },
    )

    db.add(db_event)
    db.commit()

    db.refresh(db_trip)
    db.refresh(selected_route)
    db.refresh(db_recommendation)

    recommendation.recommendation_id = str(db_recommendation.id)

    return TripStartResponse(
        trip_id=str(db_trip.id),
        status="active",
        selected_route=RouteDTO(
            route_id=str(selected_route.id),
            distance_km=float(selected_route.distance_km or 0),
            duration_minutes=int(selected_route.duration_minutes or 0),
            traffic_duration_minutes=int(selected_route.traffic_duration_minutes or 0),
            estimated_fuel_liters=(
                float(selected_route.estimated_fuel_liters)
                if selected_route.estimated_fuel_liters is not None
                else None
            ),
            polyline=selected_route.polyline,
        ),
        next_recommendation=recommendation,
    )


def update_trip(payload: TripUpdateRequest, db: Session) -> TripUpdateResponse:
    trip_id = to_uuid(payload.trip_id)

    trip = db.query(dbm.Trip).filter(dbm.Trip.id == trip_id).first()
    if trip is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trip not found.",
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

    should_refresh = should_refresh_recommendation(payload.vehicle_state)

    if should_refresh:
        recommendation = build_recommendation(
            recommendation_id=None,
            vehicle_state=payload.vehicle_state,
        )

        db_recommendation = dbm.Recommendation(
            trip_id=trip.id,
            recommendation_type="stop",
            final_score=0.75,
            explanation=recommendation.reason,
            scoring_breakdown={
                "source": "trip_update",
                "refresh_reason": "vehicle_state_changed",
            },
        )

        db.add(db_recommendation)
        db.flush()

        location = get_or_create_recommendation_location(recommendation, db)

        db_item = dbm.RecommendationItem(
            recommendation_id=db_recommendation.id,
            location_id=location.id,
            item_type=recommendation.type,
            rank_position=1,
            score=0.75,
            reason=recommendation.reason,
        )

        db.add(db_item)

        db_event = dbm.RecommendationEvent(
            user_id=trip.user_id,
            trip_id=trip.id,
            recommendation_id=db_recommendation.id,
            recommendation_item_id=db_item.id,
            event_type="generated",
            event_metadata={"source": "trip_update"},
        )

        db.add(db_event)

    db.commit()

    return TripUpdateResponse(
        trip_id=str(trip.id),
        status=trip.status,
        should_refresh_recommendation=should_refresh,
    )


def should_refresh_recommendation(vehicle_state: VehicleStateDTO) -> bool:
    fuel_is_low = (
        vehicle_state.fuel_level_percent is not None
        and vehicle_state.fuel_level_percent < 20
    )
    tire_pressure_changed = vehicle_state.tire_pressure_status != "ok"

    return fuel_is_low or tire_pressure_changed or vehicle_state.engine_warning is True


def create_mock_route_for_trip(trip: dbm.Trip, db: Session) -> dbm.RouteOption:
    route = dbm.RouteOption(
        trip_id=trip.id,
        provider="mock",
        route_name="Mock recommended route",
        polyline=None,
        distance_km=168,
        duration_minutes=155,
        traffic_duration_minutes=172,
        estimated_energy_kwh=None,
        estimated_fuel_liters=12.1,
        estimated_cost=80,
        toll_cost=0,
        route_score=0.80,
        is_selected=True,
        scoring_breakdown={
            "source": "mock_route_until_google_routes_integration"
        },
    )

    db.add(route)
    db.flush()

    return route


def get_or_create_recommendation_location(
    recommendation,
    db: Session,
) -> dbm.Location:
    existing = (
        db.query(dbm.Location)
        .filter(dbm.Location.name == recommendation.location.name)
        .first()
    )

    if existing:
        return existing

    location_type = map_recommendation_type_to_location_type(recommendation.type)

    location = dbm.Location(
        name=recommendation.location.name,
        location_type=location_type,
        latitude=recommendation.location.lat,
        longitude=recommendation.location.lng,
        city=None,
        country="Romania",
        amenities=[],
    )

    db.add(location)
    db.flush()

    return location


def map_recommendation_type_to_location_type(recommendation_type: str) -> str:
    mapping = {
        "fuel_stop": "fuel",
        "charging_stop": "charging",
        "service_stop": "service",
        "rest_stop": "rest",
        "food_stop": "restaurant",
        "parking": "parking",
        "partner_offer": "other",
    }

    return mapping.get(recommendation_type, "other")
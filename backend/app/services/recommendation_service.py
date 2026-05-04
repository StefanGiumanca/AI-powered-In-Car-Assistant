from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas import (
    NextActionRequest,
    NextActionResponse,
    RecommendationEventRequest,
    RecommendationEventResponse,
)


def to_uuid(value: str) -> UUID:
    try:
        return UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid UUID: {value}",
        )


def get_next_action(
    payload: NextActionRequest,
    db: Session,
) -> NextActionResponse:
    trip_id = to_uuid(payload.trip_id)

    trip = db.query(dbm.Trip).filter(dbm.Trip.id == trip_id).first()
    if trip is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trip not found.",
        )

    recommendation = (
        db.query(dbm.Recommendation)
        .filter(dbm.Recommendation.trip_id == trip_id)
        .order_by(dbm.Recommendation.created_at.desc())
        .first()
    )

    if recommendation is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No recommendation is available for this trip.",
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

    location = item.location

    return NextActionResponse(
        recommendation_id=str(recommendation.id),
        action=map_item_type_to_action(item.item_type),
        title=build_title_from_item_type(item.item_type),
        reason=item.reason or recommendation.explanation or "Recommended next action.",
        priority=map_score_to_priority(float(item.score or 0)),
        confidence=float(item.score or recommendation.final_score or 0.75),
        detour_minutes=0,
        location={
            "id": str(location.id) if location else "unknown",
            "name": location.name if location else "Unknown location",
            "lat": float(location.latitude) if location else 0,
            "lng": float(location.longitude) if location else 0,
        },
        offer={
            "id": str(item.partner_offer_id) if item.partner_offer_id else "offer_none",
            "title": "No active offer",
            "loyalty_points": int(item.loyalty_score or 0),
        },
    )


def save_recommendation_event(
    payload: RecommendationEventRequest,
    db: Session,
) -> RecommendationEventResponse:
    trip_id = to_uuid(payload.trip_id)
    recommendation_id = to_uuid(payload.recommendation_id)

    trip = db.query(dbm.Trip).filter(dbm.Trip.id == trip_id).first()
    if trip is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trip not found.",
        )

    recommendation = (
        db.query(dbm.Recommendation)
        .filter(dbm.Recommendation.id == recommendation_id)
        .first()
    )
    if recommendation is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Recommendation not found.",
        )

    event = dbm.RecommendationEvent(
        user_id=trip.user_id,
        trip_id=trip.id,
        recommendation_id=recommendation.id,
        event_type=payload.event_type,
        event_metadata={
            "source": "frontend",
        },
    )

    db.add(event)

    if payload.event_type == "accepted":
        first_item = (
            db.query(dbm.RecommendationItem)
            .filter(dbm.RecommendationItem.recommendation_id == recommendation.id)
            .order_by(dbm.RecommendationItem.rank_position.asc())
            .first()
        )

        if first_item:
            first_item.accepted = True

    db.commit()

    return RecommendationEventResponse(saved=True)


def map_item_type_to_action(item_type: str) -> str:
    mapping = {
        "fuel_stop": "STOP_FOR_FUEL",
        "charging_stop": "STOP_FOR_CHARGE",
        "service_center": "STOP_FOR_SERVICE",
        "service_stop": "STOP_FOR_SERVICE",
        "rest_stop": "TAKE_BREAK",
        "food_stop": "TAKE_BREAK",
        "parking": "CONTINUE_TRIP",
        "route": "CONTINUE_TRIP",
    }

    return mapping.get(item_type, "CONTINUE_TRIP")


def build_title_from_item_type(item_type: str) -> str:
    mapping = {
        "fuel_stop": "Recommended fuel stop",
        "charging_stop": "Recommended charging stop",
        "service_center": "Recommended service stop",
        "service_stop": "Recommended service stop",
        "rest_stop": "Recommended rest stop",
        "food_stop": "Recommended food stop",
        "parking": "Recommended parking option",
        "route": "Continue on selected route",
    }

    return mapping.get(item_type, "Recommended next action")


def map_score_to_priority(score: float) -> str:
    if score >= 0.80:
        return "high"
    if score >= 0.50:
        return "medium"
    return "low"

from app.schemas.recommendations import RecommendationDTO, NamedLocationDTO, OfferDTO


def build_recommendation(recommendation_id=None, vehicle_state=None) -> RecommendationDTO:
    """
    Temporary recommendation builder until the real scoring engine is implemented.
    """

    fuel_level = getattr(vehicle_state, "fuel_level_percent", None)
    battery_soc = getattr(vehicle_state, "battery_soc_percent", None)
    tire_pressure = getattr(vehicle_state, "tire_pressure_status", "ok")
    engine_warning = getattr(vehicle_state, "engine_warning", False)

    if battery_soc is not None and battery_soc < 25:
        return RecommendationDTO(
            recommendation_id=str(recommendation_id) if recommendation_id else "temporary",
            type="charging_stop",
            title="Recommended charging stop",
            reason="Battery level is low. A charging stop is recommended.",
            priority="high",
            confidence=0.85,
            detour_minutes=8,
            location=NamedLocationDTO(
                id="charge_location",
                name="OMV Charging Station",
                lat=44.4268,
                lng=26.1025,
            ),
            offer=OfferDTO(
                id="offer_charge",
                title="Earn 80 loyalty points",
                loyalty_points=80,
            ),
        )

    if fuel_level is not None and fuel_level < 20:
        return RecommendationDTO(
            recommendation_id=str(recommendation_id) if recommendation_id else "temporary",
            type="fuel_stop",
            title="Recommended fuel stop",
            reason="Fuel level is low. A fuel stop is recommended.",
            priority="high",
            confidence=0.85,
            detour_minutes=6,
            location=NamedLocationDTO(
                id="fuel_location",
                name="OMV Fuel Station",
                lat=44.4268,
                lng=26.1025,
            ),
            offer=OfferDTO(
                id="offer_fuel",
                title="Earn 50 loyalty points",
                loyalty_points=50,
            ),
        )

    if tire_pressure != "ok" or engine_warning is True:
        return RecommendationDTO(
            recommendation_id=str(recommendation_id) if recommendation_id else "temporary",
            type="service_stop",
            title="Recommended service check",
            reason="Vehicle status indicates a possible service need.",
            priority="medium",
            confidence=0.75,
            detour_minutes=10,
            location=NamedLocationDTO(
                id="service_location",
                name="Authorized Service Center",
                lat=44.4268,
                lng=26.1025,
            ),
            offer=OfferDTO(
                id="offer_service",
                title="Service inspection discount",
                loyalty_points=100,
            ),
        )

    return RecommendationDTO(
        recommendation_id=str(recommendation_id) if recommendation_id else "temporary",
        type="rest_stop",
        title="Continue trip",
        reason="No urgent stop is required right now.",
        priority="low",
        confidence=0.65,
        detour_minutes=0,
        location=NamedLocationDTO(
            id="continue_route",
            name="Current route",
            lat=44.4268,
            lng=26.1025,
        ),
        offer=OfferDTO(
            id="offer_none",
            title="No active offer",
            loyalty_points=0,
        ),
    )


def build_next_action(recommendation: RecommendationDTO) -> RecommendationDTO:
    """
    Temporary passthrough helper.
    """
    return recommendation

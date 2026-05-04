from uuid import UUID

from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas.query_agent import (
    RecommendationCandidateResponse,
    RecommendationQueryRequest,
    RecommendationQueryResponse,
)
from app.services.location_cache_service import get_or_create_location_from_candidate
from app.services.partner_matching_service import (
    find_best_offer_for_partner,
    find_partner_for_candidate,
)
from app.services.place_discovery_service import discover_places_for_interpretation
from app.services.place_ranking_service import rank_candidates
from app.services.query_agent import interpret_driver_query
from app.services.search_radius_service import determine_initial_radius, get_next_radius


async def recommend_from_driver_query(
    db: Session,
    payload: RecommendationQueryRequest,
    radius_override_meters: int | None = None,
) -> RecommendationQueryResponse:
    user = db.query(dbm.User).filter(dbm.User.id == UUID(payload.user_id)).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found.")

    vehicle = db.query(dbm.Vehicle).filter(dbm.Vehicle.id == UUID(payload.vehicle_id)).first()
    if vehicle is None:
        raise HTTPException(status_code=404, detail="Vehicle not found.")

    driver_profile = (
        db.query(dbm.DriverProfile)
        .filter(dbm.DriverProfile.id == UUID(payload.driver_profile_id))
        .first()
    )
    if driver_profile is None:
        raise HTTPException(status_code=404, detail="Driver profile not found.")

    vehicle_state = (
        db.query(dbm.VehicleStateSnapshot)
        .filter(dbm.VehicleStateSnapshot.vehicle_id == vehicle.id)
        .order_by(dbm.VehicleStateSnapshot.captured_at.desc())
        .first()
    )

    interpretation = interpret_driver_query(payload.query)

    if interpretation.intent == "unknown" or interpretation.google_place_type is None:
        return RecommendationQueryResponse(
            interpretation=interpretation,
            radius_meters=0,
            strict_match_found=False,
            message="I could not understand what type of place you want.",
            candidates=[],
            can_expand_radius=False,
            next_radius_meters=None,
        )

    radius_meters = radius_override_meters or determine_initial_radius(
        interpretation=interpretation,
        vehicle=vehicle,
        vehicle_state=vehicle_state,
    )

    candidates = await discover_places_for_interpretation(
        interpretation=interpretation,
        latitude=payload.latitude,
        longitude=payload.longitude,
        radius_meters=radius_meters,
        max_result_count=10,
    )

    partner_offer_pairs = {}

    for candidate in candidates:
        partner = find_partner_for_candidate(db, candidate)
        offer = find_best_offer_for_partner(db, partner)
        partner_offer_pairs[candidate.google_place_id] = (partner, offer)

    ranked = rank_candidates(
        candidates=candidates,
        interpretation=interpretation,
        driver_profile=driver_profile,
        partner_offer_pairs=partner_offer_pairs,
    )

    top_ranked = ranked[:5]

    strict_match_found = any(item.matches_requested_brand for item in top_ranked)

    message = build_result_message(
        interpretation=interpretation,
        strict_match_found=strict_match_found,
        radius_meters=radius_meters,
        result_count=len(top_ranked),
    )

    response_candidates = [
        RecommendationCandidateResponse(
            google_place_id=item.candidate.google_place_id,
            name=item.candidate.name,
            address=item.candidate.address,
            latitude=item.candidate.latitude,
            longitude=item.candidate.longitude,
            rating=item.candidate.rating,
            score=item.score,
            reason=item.reason,
            partner_name=item.partner.name if item.partner else None,
            offer_title=item.offer.title if item.offer else None,
            loyalty_points=item.offer.loyalty_points if item.offer else None,
            matches_requested_brand=item.matches_requested_brand,
        )
        for item in top_ranked
    ]

    return RecommendationQueryResponse(
        interpretation=interpretation,
        radius_meters=radius_meters,
        strict_match_found=strict_match_found,
        message=message,
        candidates=response_candidates,
        can_expand_radius=get_next_radius(radius_meters) is not None,
        next_radius_meters=get_next_radius(radius_meters),
    )


def build_result_message(
    interpretation,
    strict_match_found: bool,
    radius_meters: int,
    result_count: int,
) -> str:
    radius_km = radius_meters / 1000

    if result_count == 0:
        return f"I did not find matching places within {radius_km:.0f} km."

    if interpretation.strict_brand and not strict_match_found:
        return (
            f"I did not find {interpretation.brand_constraint} within {radius_km:.0f} km. "
            "I found nearby alternatives. You can select one of them or expand the search radius."
        )

    if interpretation.strict_brand and strict_match_found:
        return f"I found {interpretation.brand_constraint} options within {radius_km:.0f} km."

    return f"I found {result_count} relevant options within {radius_km:.0f} km."
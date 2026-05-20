from types import SimpleNamespace
from uuid import UUID

from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas.query_agent import (
    PlaceSearchIntentResponse,
    RecommendationCandidateResponse,
    RecommendationQueryRequest,
    RecommendationQueryResponse,
)
from app.services.partner_matching_service import (
    find_best_offer_for_partner,
    find_partner_for_candidate,
)
from app.services.place_discovery_service import (
    discover_places_for_intent,
    discover_places_for_interpretation,
)
from app.services.place_ranking_service import (
    PlaceSearchIntent,
    ScoredCandidate,
    rank_candidates,
    rank_candidates_by_intent,
)
from app.services.query_agent import extract_place_search_intent, interpret_driver_query
from app.services.search_radius_service import determine_initial_radius, get_next_radius


async def recommend_from_driver_query(
    db: Session,
    payload: RecommendationQueryRequest,
    radius_override_meters: int | None = None,
) -> RecommendationQueryResponse:
    user = db.query(dbm.User).filter(dbm.User.id == UUID(payload.user_id)).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found.")

    vehicle = None
    if payload.vehicle_id:
        vehicle = db.query(dbm.Vehicle).filter(dbm.Vehicle.id == UUID(payload.vehicle_id)).first()

    if vehicle is None:
        vehicle = (
            db.query(dbm.Vehicle)
            .filter(dbm.Vehicle.user_id == user.id)
            .first()
        )

    driver_profile = None
    if payload.driver_profile_id:
        driver_profile = (
            db.query(dbm.DriverProfile)
            .filter(dbm.DriverProfile.id == UUID(payload.driver_profile_id))
            .first()
        )

    if driver_profile is None:
        driver_profile = (
            db.query(dbm.DriverProfile)
            .filter(dbm.DriverProfile.user_id == user.id)
            .first()
        )

    if driver_profile is None:
        driver_profile = SimpleNamespace(
            preferred_brands=[],
            preferred_amenities=[],
            profile_type="balanced",
        )

    vehicle_state = None
    if vehicle is not None:
        vehicle_state = (
            db.query(dbm.VehicleStateSnapshot)
            .filter(dbm.VehicleStateSnapshot.vehicle_id == vehicle.id)
            .order_by(dbm.VehicleStateSnapshot.captured_at.desc())
            .first()
        )

    interpretation = interpret_driver_query(payload.query)
    radius_meters = (
        radius_override_meters
        or (
            determine_initial_radius(
                interpretation=interpretation,
                vehicle=vehicle,
                vehicle_state=vehicle_state,
            )
            if vehicle is not None
            else interpretation.radius_meters
        )
    )

    try:
        extracted = await extract_place_search_intent(payload.query)
    except Exception:
        return await recommend_with_rule_based_flow(
            db=db,
            payload=payload,
            interpretation=interpretation,
            driver_profile=driver_profile,
            radius_meters=radius_meters,
        )

    candidates = await discover_places_for_intent(
        intent=extracted.intent,
        search_text_queries=extracted.search_text_queries,
        latitude=payload.latitude,
        longitude=payload.longitude,
        radius_meters=radius_meters,
        max_result_count=10,
    )

    ranked = rank_candidates_by_intent(
        candidates=candidates,
        intent=extracted.intent,
        user_preferences={
            "favorite_brands": driver_profile.preferred_brands or [],
            "profile_type": driver_profile.profile_type,
        },
        partner_offer_pairs=build_partner_offer_pairs(db, candidates),
        requested_brand=extracted.requested_brand,
        driver_profile=driver_profile,
    )

    top_ranked = ranked[:3]
    strict_match_found = any(item.matches_requested_brand for item in top_ranked)

    return RecommendationQueryResponse(
        interpretation=interpretation,
        place_intent=build_place_intent_response(extracted.intent),
        requested_brand=extracted.requested_brand,
        search_text_queries=extracted.search_text_queries,
        radius_meters=radius_meters,
        strict_match_found=strict_match_found,
        message=build_ai_result_message(
            intent=extracted.intent,
            requested_brand=extracted.requested_brand,
            radius_meters=radius_meters,
            result_count=len(top_ranked),
        ),
        candidates=build_candidate_responses(top_ranked),
        can_expand_radius=get_next_radius(radius_meters) is not None,
        next_radius_meters=get_next_radius(radius_meters),
    )


async def recommend_with_rule_based_flow(
    db: Session,
    payload: RecommendationQueryRequest,
    interpretation,
    driver_profile: dbm.DriverProfile,
    radius_meters: int,
) -> RecommendationQueryResponse:
    if interpretation.intent == "unknown" or interpretation.google_place_type is None:
        return RecommendationQueryResponse(
            interpretation=interpretation,
            place_intent=None,
            requested_brand=None,
            search_text_queries=[],
            radius_meters=0,
            strict_match_found=False,
            message="I could not understand what type of place you want.",
            candidates=[],
            can_expand_radius=False,
            next_radius_meters=None,
        )

    candidates = await discover_places_for_interpretation(
        interpretation=interpretation,
        latitude=payload.latitude,
        longitude=payload.longitude,
        radius_meters=radius_meters,
        max_result_count=10,
    )

    ranked = rank_candidates(
        candidates=candidates,
        interpretation=interpretation,
        driver_profile=driver_profile,
        partner_offer_pairs=build_partner_offer_pairs(db, candidates),
    )

    top_ranked = ranked[:3]

    strict_match_found = any(item.matches_requested_brand for item in top_ranked)

    message = build_result_message(
        interpretation=interpretation,
        strict_match_found=strict_match_found,
        radius_meters=radius_meters,
        result_count=len(top_ranked),
    )

    return RecommendationQueryResponse(
        interpretation=interpretation,
        place_intent=None,
        requested_brand=interpretation.brand_constraint,
        search_text_queries=[],
        radius_meters=radius_meters,
        strict_match_found=strict_match_found,
        message=message,
        candidates=build_candidate_responses(top_ranked),
        can_expand_radius=get_next_radius(radius_meters) is not None,
        next_radius_meters=get_next_radius(radius_meters),
    )


def build_partner_offer_pairs(
    db: Session,
    candidates,
) -> dict[str, tuple[dbm.Partner | None, dbm.PartnerOffer | None]]:
    partner_offer_pairs = {}

    for candidate in candidates:
        partner = find_partner_for_candidate(db, candidate)
        offer = find_best_offer_for_partner(db, partner)
        partner_offer_pairs[candidate.google_place_id] = (partner, offer)

    return partner_offer_pairs


def build_candidate_responses(
    ranked_candidates: list[ScoredCandidate],
) -> list[RecommendationCandidateResponse]:
    return [
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
        for item in ranked_candidates
    ]


def build_place_intent_response(intent: PlaceSearchIntent) -> PlaceSearchIntentResponse:
    return PlaceSearchIntentResponse(
        primary_query=intent.primary_query,
        provider_types=intent.provider_types,
        strong_positive_signals=intent.strong_positive_signals,
        positive_signals=intent.positive_signals,
        negative_signals=intent.negative_signals,
        excluded_types=intent.excluded_types,
        strictness=intent.strictness,
        open_now_required=intent.open_now_required,
        search_strategy=intent.search_strategy,
        requires_specific_match=intent.requires_specific_match,
    )


def build_ai_result_message(
    intent: PlaceSearchIntent,
    requested_brand: str | None,
    radius_meters: int,
    result_count: int,
) -> str:
    radius_km = radius_meters / 1000

    if result_count == 0:
        return f"I did not find matching places for {intent.primary_query} within {radius_km:.0f} km."

    if requested_brand:
        return f"I found {result_count} relevant {requested_brand} options within {radius_km:.0f} km."

    return f"I found {result_count} relevant options for {intent.primary_query} within {radius_km:.0f} km."


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

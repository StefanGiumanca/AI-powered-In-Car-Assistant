from dataclasses import dataclass
from typing import Any

from app.db import models as dbm
from app.schemas.query_agent import QueryInterpretation
from app.services.place_candidate_mapper import PlaceCandidate


@dataclass
class ScoredCandidate:
    candidate: PlaceCandidate
    score: float
    reason: str
    reasons: list[str]
    partner: dbm.Partner | None = None
    offer: dbm.PartnerOffer | None = None
    matches_requested_brand: bool = False
    is_partner: bool = False
    distance_km: float | None = None


def rank_candidates(
    candidates: list[PlaceCandidate],
    interpretation: QueryInterpretation,
    driver_profile: dbm.DriverProfile,
    partner_offer_pairs: dict[str, tuple[dbm.Partner | None, dbm.PartnerOffer | None]],
    distance_by_place_id: dict[str, float] | None = None,
) -> list[ScoredCandidate]:
    """
    Scores and sorts Google Places candidates.

    V1 ranking factors:
    - open now
    - partner/sponsor match
    - user preferred brand match
    - requested brand match
    - distance, if available
    - rating
    - review count
    - active offer / loyalty points

    Notes:
    - distance_by_place_id is optional because the current Places flow may not yet
      calculate accurate route detour or distance.
    - Once Google Routes API is added, pass real distance/detour here.
    """

    scored: list[ScoredCandidate] = []
    distance_by_place_id = distance_by_place_id or {}

    for candidate in candidates:
        partner, offer = partner_offer_pairs.get(
            candidate.google_place_id,
            (None, None),
        )

        distance_km = distance_by_place_id.get(candidate.google_place_id)

        scored_candidate = score_place_candidate(
            candidate=candidate,
            interpretation=interpretation,
            driver_profile=driver_profile,
            partner=partner,
            offer=offer,
            distance_km=distance_km,
        )

        scored.append(scored_candidate)

    return sorted(scored, key=lambda item: item.score, reverse=True)


def score_place_candidate(
    candidate: PlaceCandidate,
    interpretation: QueryInterpretation,
    driver_profile: dbm.DriverProfile,
    partner: dbm.Partner | None,
    offer: dbm.PartnerOffer | None,
    distance_km: float | None = None,
) -> ScoredCandidate:
    score = 0.0
    reasons: list[str] = []

    name = candidate.name.lower()
    rating = float(candidate.rating or 0)
    reviews = int(candidate.rating_count or 0)

    requested_brand = interpretation.brand_constraint
    favorite_brands = driver_profile.preferred_brands or []
    partner_brand = partner.name if partner else None

    # -----------------------------------------------------------------------
    # 1. Opening status
    # -----------------------------------------------------------------------

    if candidate.is_open is True:
        score += 50
        reasons.append("Open now")
    elif candidate.is_open is False:
        score -= 50
        reasons.append("Closed now")
    else:
        reasons.append("Opening status unknown")

    # -----------------------------------------------------------------------
    # 2. Partner / sponsor match
    # -----------------------------------------------------------------------

    is_partner = partner is not None

    if is_partner:
        score += 30
        reasons.append(f"Partner / sponsor location: {partner.name}")

    # -----------------------------------------------------------------------
    # 3. User preferred brands
    # -----------------------------------------------------------------------

    is_user_preference = any(
        str(brand).lower() in name
        for brand in favorite_brands
    )

    if is_user_preference:
        score += 20
        reasons.append("Matches user preferred brand")

    # -----------------------------------------------------------------------
    # 4. Requested brand from query
    # -----------------------------------------------------------------------

    matches_requested_brand = candidate_matches_brand(
        candidate=candidate,
        brand_constraint=requested_brand,
    )

    if requested_brand:
        if matches_requested_brand:
            score += 40
            reasons.append(f"Matches requested brand: {requested_brand}")
        else:
            if interpretation.strict_brand:
                score -= 25
                reasons.append(f"Does not match requested brand: {requested_brand}")
            else:
                reasons.append(f"Does not match requested brand: {requested_brand}")

    # -----------------------------------------------------------------------
    # 5. Distance score, if available
    # -----------------------------------------------------------------------

    if distance_km is not None:
        if distance_km <= 1:
            score += 8
            reasons.append("Very close")
        elif distance_km <= 3:
            score += 5
            reasons.append("Nearby")
        elif distance_km <= 5:
            score += 2
            reasons.append("Acceptable distance")
        else:
            score -= 3
            reasons.append("Farther away")

    # -----------------------------------------------------------------------
    # 6. Rating score
    # -----------------------------------------------------------------------

    if rating:
        rating_bonus = (rating / 5) * 6
        score += rating_bonus
        reasons.append(f"Rated {rating}/5")

    # -----------------------------------------------------------------------
    # 7. Review count score
    # -----------------------------------------------------------------------

    if reviews >= 1000:
        score += 4
        reasons.append("Highly reviewed")
    elif reviews >= 500:
        score += 2
        reasons.append("Well reviewed")
    elif reviews >= 100:
        score += 1
        reasons.append("Has enough reviews")

    # -----------------------------------------------------------------------
    # 8. Offer / loyalty score
    # -----------------------------------------------------------------------

    if offer is not None:
        score += 10
        reasons.append(f"Has active offer: {offer.title}")

        if offer.loyalty_points:
            loyalty_bonus = min(float(offer.loyalty_points) / 50, 10)
            score += loyalty_bonus
            reasons.append(f"Includes {offer.loyalty_points} loyalty points")

        if offer.discount_percent:
            discount_bonus = min(float(offer.discount_percent) / 2, 10)
            score += discount_bonus
            reasons.append(f"Includes {offer.discount_percent}% discount")

        if offer.fixed_discount_amount:
            fixed_discount_bonus = min(float(offer.fixed_discount_amount), 10)
            score += fixed_discount_bonus
            reasons.append(f"Includes fixed discount of {offer.fixed_discount_amount}")

    # -----------------------------------------------------------------------
    # 9. Profile-type adjustments
    # -----------------------------------------------------------------------

    score += calculate_profile_type_bonus(
        candidate=candidate,
        driver_profile=driver_profile,
        distance_km=distance_km,
        offer=offer,
    )

    reason = build_reason_from_reasons(reasons)

    return ScoredCandidate(
        candidate=candidate,
        score=round(score, 2),
        reason=reason,
        reasons=reasons,
        partner=partner,
        offer=offer,
        matches_requested_brand=matches_requested_brand,
        is_partner=is_partner,
        distance_km=distance_km,
    )


def calculate_profile_type_bonus(
    candidate: PlaceCandidate,
    driver_profile: dbm.DriverProfile,
    distance_km: float | None,
    offer: dbm.PartnerOffer | None,
) -> float:
    """
    Adds small behavior-specific bonuses based on driver profile.

    This should not dominate hard user intent.
    Example: if user explicitly asks for PETROM, brand matching is still more important.
    """

    profile_type = driver_profile.profile_type
    candidate_types = set(candidate.types or [])
    bonus = 0.0

    if profile_type == "family":
        family_types = {
            "restaurant",
            "cafe",
            "parking",
            "shopping_mall",
            "supermarket",
        }

        if candidate_types.intersection(family_types):
            bonus += 6

    elif profile_type == "business":
        if distance_km is not None and distance_km <= 3:
            bonus += 8

        if "cafe" in candidate_types:
            bonus += 3

    elif profile_type == "cheapest":
        if offer is not None:
            bonus += 8

        if offer and (offer.discount_percent or offer.fixed_discount_amount):
            bonus += 5

    elif profile_type == "scenic":
        scenic_types = {
            "tourist_attraction",
            "park",
            "cafe",
            "restaurant",
        }

        if candidate_types.intersection(scenic_types):
            bonus += 5

    elif profile_type == "fastest":
        if distance_km is not None:
            if distance_km <= 1:
                bonus += 10
            elif distance_km <= 3:
                bonus += 6
            elif distance_km <= 5:
                bonus += 2

    return bonus


def candidate_matches_brand(
    candidate: PlaceCandidate,
    brand_constraint: str | None,
) -> bool:
    if not brand_constraint:
        return False

    return brand_constraint.lower() in candidate.name.lower()


def build_reason_from_reasons(reasons: list[str]) -> str:
    if not reasons:
        return "Recommended based on location relevance and availability."

    return "Recommended because: " + "; ".join(reasons) + "."


# ---------------------------------------------------------------------------
# Optional compatibility functions
# ---------------------------------------------------------------------------
# These keep compatibility with your previous function names if another file
# imports score_and_sort_place_candidates or score_place_candidate-like behavior.
# Prefer rank_candidates(...) in the main recommendation flow.
# ---------------------------------------------------------------------------

def score_and_sort_place_candidates(
    places: list[PlaceCandidate],
    user_preferences: dict[str, Any],
    partner_brands: list[str],
    requested_brand: str | None = None,
) -> list[ScoredCandidate]:
    """
    Compatibility helper for older/simple tests.

    This does not use DB Partner or PartnerOffer objects.
    Main production flow should use rank_candidates(...).
    """

    fake_profile = build_fake_driver_profile_from_preferences(user_preferences)
    fake_interpretation = build_fake_interpretation(requested_brand)

    partner_offer_pairs: dict[str, tuple[dbm.Partner | None, dbm.PartnerOffer | None]] = {}

    return rank_candidates(
        candidates=places,
        interpretation=fake_interpretation,
        driver_profile=fake_profile,
        partner_offer_pairs=partner_offer_pairs,
    )


def build_fake_driver_profile_from_preferences(user_preferences: dict[str, Any]):
    class FakeDriverProfile:
        profile_type = user_preferences.get("profile_type", "balanced")
        preferred_brands = user_preferences.get("favorite_brands", [])
        preferred_amenities = user_preferences.get("preferred_amenities", [])

    return FakeDriverProfile()


def build_fake_interpretation(requested_brand: str | None):
    class FakeInterpretation:
        brand_constraint = requested_brand
        strict_brand = requested_brand is not None

    return FakeInterpretation()
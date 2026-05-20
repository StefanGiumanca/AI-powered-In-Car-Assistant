from dataclasses import dataclass, field
from typing import Any, Literal

from app.db import models as dbm
from app.schemas.query_agent import QueryInterpretation
from app.services.place_candidate_mapper import PlaceCandidate


Strictness = Literal["low", "medium", "high"]
SearchStrategy = Literal["nearby_type", "text_strict", "hybrid"]

GENERIC_PROVIDER_TYPES = {
    "restaurant",
    "food",
    "store",
    "clothing_store",
    "point_of_interest",
    "establishment",
}


@dataclass(frozen=True)
class PlaceSearchIntent:
    primary_query: str
    provider_types: list[str] = field(default_factory=list)
    strong_positive_signals: list[str] = field(default_factory=list)
    positive_signals: list[str] = field(default_factory=list)
    negative_signals: list[str] = field(default_factory=list)
    excluded_types: list[str] = field(default_factory=list)
    strictness: Strictness = "medium"
    open_now_required: bool = False
    search_strategy: SearchStrategy = "hybrid"
    requires_specific_match: bool = True


@dataclass
class ScoredCandidate:
    candidate: PlaceCandidate
    score: float
    relevance_score: float
    ranking_score: float
    accepted: bool
    reason: str
    reasons: list[str]
    rejection_reason: str | None = None
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
    Backward-compatible entrypoint for the current rule-based QueryInterpretation.

    The scoring core now works from PlaceSearchIntent, the same shape we expect
    from the AI query agent. Once the AI flow is wired in, call
    rank_candidates_by_intent(...) directly.
    """

    intent = build_intent_from_interpretation(interpretation)
    user_preferences = {
        "favorite_brands": driver_profile.preferred_brands or [],
        "profile_type": driver_profile.profile_type,
    }

    return rank_candidates_by_intent(
        candidates=candidates,
        intent=intent,
        user_preferences=user_preferences,
        partner_offer_pairs=partner_offer_pairs,
        requested_brand=interpretation.brand_constraint,
        driver_profile=driver_profile,
        distance_by_place_id=distance_by_place_id,
    )


def rank_candidates_by_intent(
    candidates: list[PlaceCandidate],
    intent: PlaceSearchIntent,
    user_preferences: dict[str, Any] | None = None,
    partner_offer_pairs: dict[str, tuple[dbm.Partner | None, dbm.PartnerOffer | None]] | None = None,
    partner_brands: list[str] | None = None,
    requested_brand: str | None = None,
    driver_profile: dbm.DriverProfile | None = None,
    distance_by_place_id: dict[str, float] | None = None,
) -> list[ScoredCandidate]:
    distance_by_place_id = distance_by_place_id or {}
    partner_offer_pairs = partner_offer_pairs or {}
    partner_brands = partner_brands or []

    scored = [
        score_place_candidate(
            place=candidate,
            intent=intent,
            user_preferences=user_preferences,
            partner_brands=partner_brands,
            requested_brand=requested_brand,
            distance_km=distance_by_place_id.get(candidate.google_place_id),
            partner=partner_offer_pairs.get(candidate.google_place_id, (None, None))[0],
            offer=partner_offer_pairs.get(candidate.google_place_id, (None, None))[1],
            driver_profile=driver_profile,
        )
        for candidate in candidates
    ]

    accepted = [item for item in scored if item.accepted]
    return sorted(accepted, key=lambda item: item.score, reverse=True)


def score_place_candidate(
    place: PlaceCandidate,
    intent: PlaceSearchIntent,
    user_preferences: dict[str, Any] | None = None,
    partner_brands: list[str] | None = None,
    requested_brand: str | None = None,
    distance_km: float | None = None,
    partner: dbm.Partner | None = None,
    offer: dbm.PartnerOffer | None = None,
    driver_profile: dbm.DriverProfile | None = None,
) -> ScoredCandidate:
    user_preferences = user_preferences or {}
    partner_brands = partner_brands or []

    reasons: list[str] = []
    relevance_score = 0.0
    ranking_score = 0.0

    name = normalize_text(place.name)
    address = normalize_text(place.address or "")
    primary_type = normalize_text(place.primary_type or "")
    types = {normalize_text(place_type) for place_type in place.types or []}
    candidate_text = " ".join([name, address, primary_type, " ".join(types)])

    matches_requested_brand = bool(
        requested_brand and normalize_text(requested_brand) in name
    )
    is_partner = partner is not None

    if intent.open_now_required and place.is_open is False:
        return rejected(
            place=place,
            reason="closed_now",
            reasons=["Closed now"],
            partner=partner,
            offer=offer,
            matches_requested_brand=matches_requested_brand,
            is_partner=is_partner,
            distance_km=distance_km,
        )

    excluded_types = {normalize_text(place_type) for place_type in intent.excluded_types}
    if primary_type in excluded_types or types.intersection(excluded_types):
        return rejected(
            place=place,
            reason="excluded_type",
            reasons=["Excluded place type"],
            partner=partner,
            offer=offer,
            matches_requested_brand=matches_requested_brand,
            is_partner=is_partner,
            distance_km=distance_km,
        )

    negative_matches = find_signal_matches(intent.negative_signals, candidate_text)
    if negative_matches and intent.strictness == "high":
        return rejected(
            place=place,
            reason="negative_signal_match",
            reasons=[f"Negative: {signal}" for signal in negative_matches],
            partner=partner,
            offer=offer,
            matches_requested_brand=matches_requested_brand,
            is_partner=is_partner,
            distance_km=distance_km,
        )

    provider_types = {normalize_text(place_type) for place_type in intent.provider_types}

    if primary_type in provider_types:
        relevance_score += 15
        reasons.append("Primary type match")

    matched_types = types.intersection(provider_types)
    if matched_types:
        relevance_score += 10
        reasons.append("Type match")

    strong_matches = find_signal_matches(intent.strong_positive_signals, candidate_text)
    for signal in strong_matches:
        relevance_score += 30
        reasons.append(f"Strong relevance: {signal}")

    positive_matches = find_signal_matches(intent.positive_signals, candidate_text)
    for signal in positive_matches:
        relevance_score += 12
        reasons.append(f"Relevance: {signal}")

    for signal in negative_matches:
        relevance_score -= 30
        reasons.append(f"Negative: {signal}")

    if matches_requested_brand:
        relevance_score += 20
        reasons.append("Matches requested brand")
    elif requested_brand:
        return rejected(
            place=place,
            reason="missing_requested_brand",
            reasons=[f"Missing requested brand: {requested_brand}"],
            partner=partner,
            offer=offer,
            matches_requested_brand=matches_requested_brand,
            is_partner=is_partner,
            distance_km=distance_km,
        )

    has_specific_provider_type_match = bool(
        (
            primary_type in provider_types
            and primary_type not in GENERIC_PROVIDER_TYPES
        )
        or matched_types.difference(GENERIC_PROVIDER_TYPES)
    )

    if has_specific_provider_type_match:
        relevance_score += 25
        reasons.append("Specific place type match")

    type_match_is_allowed = bool(
        not intent.requires_specific_match
        and not requested_brand
        and (primary_type in provider_types or matched_types)
    )

    has_specific_relevance_signal = bool(
        matches_requested_brand
        or strong_matches
        or positive_matches
        or has_specific_provider_type_match
        or type_match_is_allowed
    )

    if not has_specific_relevance_signal:
        return rejected(
            place=place,
            reason="missing_specific_relevance_signal",
            reasons=reasons,
            partner=partner,
            offer=offer,
            matches_requested_brand=matches_requested_brand,
            is_partner=is_partner,
            distance_km=distance_km,
        )

    if place.is_open is True:
        ranking_score += 15
        reasons.append("Open now")
    elif place.is_open is False:
        ranking_score -= 15
        reasons.append("Closed now")

    ranking_score += score_distance(distance_km, reasons)
    ranking_score += score_rating(place, reasons)
    ranking_score += score_reviews(place, reasons)
    ranking_score += score_brand_affinity(
        name=name,
        user_preferences=user_preferences,
        partner_brands=partner_brands,
        reasons=reasons,
    )
    ranking_score += score_partner_offer(partner, offer, reasons)

    if driver_profile is not None:
        ranking_score += calculate_profile_type_bonus(
            candidate=place,
            driver_profile=driver_profile,
            distance_km=distance_km,
            offer=offer,
        )

    threshold = relevance_threshold(intent.strictness)
    accepted = relevance_score >= threshold

    return ScoredCandidate(
        candidate=place,
        score=round(relevance_score + ranking_score, 2),
        relevance_score=round(relevance_score, 2),
        ranking_score=round(ranking_score, 2),
        accepted=accepted,
        reason=build_reason_from_reasons(reasons),
        reasons=reasons,
        rejection_reason=None if accepted else "below_relevance_threshold",
        partner=partner,
        offer=offer,
        matches_requested_brand=matches_requested_brand,
        is_partner=is_partner,
        distance_km=distance_km,
    )


def build_intent_from_interpretation(
    interpretation: QueryInterpretation,
) -> PlaceSearchIntent:
    provider_types = [interpretation.google_place_type] if interpretation.google_place_type else []
    strong_positive_signals: list[str] = []
    positive_signals = build_positive_signals(interpretation)
    negative_signals = build_negative_signals(interpretation)
    excluded_types = build_excluded_types(interpretation)

    if interpretation.brand_constraint:
        strong_positive_signals.append(interpretation.brand_constraint)

    if interpretation.food_query:
        strong_positive_signals.append(interpretation.food_query)

    strictness: Strictness = "high" if (
        interpretation.strict_brand or interpretation.food_query
    ) else "medium"

    return PlaceSearchIntent(
        primary_query=interpretation.food_query
        or interpretation.brand_constraint
        or interpretation.place_category,
        provider_types=unique_signals(provider_types),
        strong_positive_signals=unique_signals(strong_positive_signals),
        positive_signals=unique_signals(positive_signals),
        negative_signals=unique_signals(negative_signals),
        excluded_types=unique_signals(excluded_types),
        strictness=strictness,
        open_now_required=False,
        search_strategy="text_strict" if interpretation.strict_brand else "nearby_type",
        requires_specific_match=bool(interpretation.strict_brand or interpretation.food_query),
    )


def build_positive_signals(interpretation: QueryInterpretation) -> list[str]:
    category_signals = {
        "fuel": ["gas", "fuel", "petrol", "benzina", "motorina", "station"],
        "charging": ["charging", "charger", "ev", "electric", "incarcare"],
        "restaurant": ["restaurant", "food", "meal", "takeaway"],
        "cafe": ["cafe", "coffee", "espresso", "latte"],
        "hotel": ["hotel", "cazare", "lodging", "stay"],
        "service": ["service", "repair", "car repair", "mechanic", "auto"],
        "parking": ["parking", "parcare"],
    }

    signals = category_signals.get(interpretation.place_category, []).copy()

    if interpretation.food_query == "shaorma":
        signals.extend(["shaorma", "shawarma", "kebab", "doner"])
    elif interpretation.food_query:
        signals.append(interpretation.food_query)

    return signals


def build_negative_signals(interpretation: QueryInterpretation) -> list[str]:
    category_negative_signals = {
        "restaurant": ["night_club", "bar", "lounge", "rooftop", "club"],
        "cafe": ["night_club", "bar", "club"],
        "fuel": ["parking", "car_wash"],
        "charging": ["parking", "car_wash"],
        "hotel": ["restaurant", "bar", "cafe"],
        "service": ["car_dealer", "car_wash", "parking"],
        "parking": ["restaurant", "bar", "hotel"],
    }

    return category_negative_signals.get(interpretation.place_category, [])


def build_excluded_types(interpretation: QueryInterpretation) -> list[str]:
    excluded_types = {
        "restaurant": ["night_club", "bar"],
        "cafe": ["night_club", "bar"],
        "hotel": ["restaurant", "bar"],
        "service": ["car_dealer", "car_wash"],
    }

    return excluded_types.get(interpretation.place_category, [])


def score_distance(distance_km: float | None, reasons: list[str]) -> float:
    if distance_km is None:
        return 0

    if distance_km <= 1:
        reasons.append("Very close")
        return 12
    if distance_km <= 3:
        reasons.append("Nearby")
        return 8
    if distance_km <= 5:
        reasons.append("Acceptable distance")
        return 4

    reasons.append("Farther away")
    return -5


def score_rating(place: PlaceCandidate, reasons: list[str]) -> float:
    rating = float(place.rating or 0)
    if not rating:
        return 0

    reasons.append(f"Rated {rating}/5")
    return min((rating / 5) * 10, 10)


def score_reviews(place: PlaceCandidate, reasons: list[str]) -> float:
    reviews = int(place.rating_count or 0)
    if reviews >= 1000:
        reasons.append("Highly reviewed")
        return 8
    if reviews >= 500:
        reasons.append("Well reviewed")
        return 5
    if reviews >= 100:
        reasons.append("Has enough reviews")
        return 2
    return 0


def score_brand_affinity(
    name: str,
    user_preferences: dict[str, Any],
    partner_brands: list[str],
    reasons: list[str],
) -> float:
    score = 0.0

    if any(normalize_text(brand) in name for brand in partner_brands):
        score += 8
        reasons.append("Partner location")

    favorite_brands = user_preferences.get("favorite_brands", [])
    if any(normalize_text(brand) in name for brand in favorite_brands):
        score += 6
        reasons.append("Matches user preference")

    return score


def score_partner_offer(
    partner: dbm.Partner | None,
    offer: dbm.PartnerOffer | None,
    reasons: list[str],
) -> float:
    score = 0.0

    if partner is not None:
        score += 8
        reasons.append(f"Partner / sponsor location: {partner.name}")

    if offer is None:
        return score

    score += 8
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

    return score


def calculate_profile_type_bonus(
    candidate: PlaceCandidate,
    driver_profile: dbm.DriverProfile,
    distance_km: float | None,
    offer: dbm.PartnerOffer | None,
) -> float:
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

    return normalize_text(brand_constraint) in normalize_text(candidate.name)


def find_signal_matches(signals: list[str], candidate_text: str) -> list[str]:
    return [
        signal
        for signal in signals
        if normalize_text(signal) and normalize_text(signal) in candidate_text
    ]


def relevance_threshold(strictness: Strictness) -> float:
    thresholds = {
        "low": 15.0,
        "medium": 30.0,
        "high": 45.0,
    }
    return thresholds[strictness]


def normalize_text(value: Any) -> str:
    return str(value).strip().lower()


def unique_signals(signals: list[str | None]) -> list[str]:
    seen: set[str] = set()
    unique: list[str] = []

    for signal in signals:
        normalized = normalize_text(signal or "")
        if normalized and normalized not in seen:
            seen.add(normalized)
            unique.append(normalized)

    return unique


def rejected(
    place: PlaceCandidate,
    reason: str,
    reasons: list[str],
    partner: dbm.Partner | None,
    offer: dbm.PartnerOffer | None,
    matches_requested_brand: bool,
    is_partner: bool,
    distance_km: float | None,
) -> ScoredCandidate:
    return ScoredCandidate(
        candidate=place,
        score=0,
        relevance_score=0,
        ranking_score=0,
        accepted=False,
        reason=build_reason_from_reasons(reasons),
        reasons=reasons,
        rejection_reason=reason,
        partner=partner,
        offer=offer,
        matches_requested_brand=matches_requested_brand,
        is_partner=is_partner,
        distance_km=distance_km,
    )


def build_reason_from_reasons(reasons: list[str]) -> str:
    if not reasons:
        return "Recommended based on location relevance and availability."

    return "Recommended because: " + "; ".join(reasons) + "."


def score_and_sort_place_candidates(
    places: list[PlaceCandidate],
    user_preferences: dict[str, Any],
    partner_brands: list[str],
    requested_brand: str | None = None,
) -> list[ScoredCandidate]:
    intent = PlaceSearchIntent(
        primary_query=requested_brand or "place",
        provider_types=[],
        strong_positive_signals=[requested_brand] if requested_brand else [],
        positive_signals=[],
        negative_signals=[],
        excluded_types=[],
        strictness="high" if requested_brand else "low",
        search_strategy="text_strict" if requested_brand else "nearby_type",
        requires_specific_match=bool(requested_brand),
    )

    return rank_candidates_by_intent(
        candidates=places,
        intent=intent,
        user_preferences=user_preferences,
        partner_brands=partner_brands,
        requested_brand=requested_brand,
    )

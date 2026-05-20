from app.schemas.query_agent import QueryInterpretation
from app.services.google_places_client import GooglePlacesClient
from app.services.place_candidate_mapper import (
    PlaceCandidate,
    map_google_place_to_candidate,
)
from app.services.place_ranking_service import PlaceSearchIntent


GENERIC_PROVIDER_TYPES = {
    "restaurant",
    "food",
    "store",
    "clothing_store",
    "point_of_interest",
    "establishment",
}


async def discover_places_for_interpretation(
    interpretation: QueryInterpretation,
    latitude: float,
    longitude: float,
    radius_meters: int,
    max_result_count: int = 10,
) -> list[PlaceCandidate]:
    if interpretation.google_place_type is None:
        return []

    client = GooglePlacesClient()

    raw_places = await client.nearby_search(
        latitude=latitude,
        longitude=longitude,
        radius_meters=radius_meters,
        included_types=[interpretation.google_place_type],
        max_result_count=max_result_count,
    )

    candidates_by_id: dict[str, PlaceCandidate] = {}

    for raw_place in raw_places:
        candidate = map_google_place_to_candidate(
            place=raw_place,
            location_type=interpretation.place_category,
        )
        candidates_by_id[candidate.google_place_id] = candidate

    return list(candidates_by_id.values())


async def discover_places_for_intent(
    intent: PlaceSearchIntent,
    search_text_queries: list[str],
    latitude: float,
    longitude: float,
    radius_meters: int,
    max_result_count: int = 10,
) -> list[PlaceCandidate]:
    client = GooglePlacesClient()
    candidates_by_id: dict[str, PlaceCandidate] = {}

    if should_run_nearby_search(intent):
        for provider_type in intent.provider_types:
            try:
                raw_places = await client.nearby_search(
                    latitude=latitude,
                    longitude=longitude,
                    radius_meters=radius_meters,
                    included_types=[provider_type],
                    max_result_count=max_result_count,
                )
            except Exception:
                raw_places = []

            for raw_place in raw_places:
                candidate = map_google_place_to_candidate(
                    place=raw_place,
                    location_type=intent.primary_query,
                )
                candidates_by_id[candidate.google_place_id] = candidate

    if intent.search_strategy in {"text_strict", "hybrid"}:
        for text_query in search_text_queries:
            try:
                raw_places = await client.text_search(
                    text_query=text_query,
                    latitude=latitude,
                    longitude=longitude,
                    radius_meters=radius_meters,
                    max_result_count=max_result_count,
                )
            except Exception:
                raw_places = []

            for raw_place in raw_places:
                candidate = map_google_place_to_candidate(
                    place=raw_place,
                    location_type=intent.primary_query,
                )
                candidates_by_id[candidate.google_place_id] = candidate

    candidates = list(candidates_by_id.values())

    if intent.search_strategy in {"text_strict", "hybrid"}:
        candidates = await enrich_candidates_with_place_details(
            client=client,
            candidates=candidates,
            location_type=intent.primary_query,
            max_details_count=max_result_count,
        )

    return candidates


def should_run_nearby_search(intent: PlaceSearchIntent) -> bool:
    if intent.search_strategy in {"nearby_type", "hybrid"}:
        return True

    return any(
        provider_type not in GENERIC_PROVIDER_TYPES
        for provider_type in intent.provider_types
    )


async def enrich_candidates_with_place_details(
    client: GooglePlacesClient,
    candidates: list[PlaceCandidate],
    location_type: str,
    max_details_count: int,
) -> list[PlaceCandidate]:
    enriched_candidates: list[PlaceCandidate] = []

    for candidate in candidates[:max_details_count]:
        try:
            raw_details = await client.place_details(candidate.google_place_id)
            enriched_candidates.append(
                map_google_place_to_candidate(
                    place=raw_details,
                    location_type=location_type,
                )
            )
        except Exception:
            enriched_candidates.append(candidate)

    enriched_candidates.extend(candidates[max_details_count:])
    return enriched_candidates

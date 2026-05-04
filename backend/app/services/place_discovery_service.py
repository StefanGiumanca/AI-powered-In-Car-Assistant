from app.schemas.query_agent import QueryInterpretation
from app.services.google_places_client import GooglePlacesClient
from app.services.place_candidate_mapper import (
    PlaceCandidate,
    map_google_place_to_candidate,
)


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
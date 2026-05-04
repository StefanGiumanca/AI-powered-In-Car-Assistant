from dataclasses import dataclass
from typing import Any


@dataclass
class PlaceCandidate:
    google_place_id: str
    name: str
    location_type: str
    address: str | None
    latitude: float
    longitude: float
    rating: float | None
    rating_count: int | None
    primary_type: str | None
    types: list[str]
    business_status: str | None
    is_open: bool | None
    raw: dict[str, Any]


def map_google_place_to_candidate(
    place: dict[str, Any],
    location_type: str,
) -> PlaceCandidate:
    display_name = place.get("displayName", {})
    location = place.get("location", {})

    return PlaceCandidate(
        google_place_id=place["id"],
        name=display_name.get("text", "Unknown place"),
        location_type=location_type,
        address=place.get("formattedAddress"),
        latitude=location.get("latitude"),
        longitude=location.get("longitude"),
        rating=place.get("rating"),
        rating_count=place.get("userRatingCount"),
        primary_type=place.get("primaryType"),
        types=place.get("types", []),
        business_status=place.get("businessStatus"),
        is_open=extract_is_open(place),
        raw=place,
    )


def extract_is_open(place: dict[str, Any]) -> bool | None:
    opening_hours = place.get("currentOpeningHours")
    if not opening_hours:
        return None

    return opening_hours.get("openNow")
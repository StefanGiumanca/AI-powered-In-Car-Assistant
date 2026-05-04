import os
from typing import Any

import httpx


class GooglePlacesClient:
    def __init__(self) -> None:
        self.api_key = os.getenv("GOOGLE_MAPS_API_KEY")
        self.base_url = os.getenv(
            "GOOGLE_PLACES_BASE_URL",
            "https://places.googleapis.com/v1",
        )

        if not self.api_key:
            raise RuntimeError("GOOGLE_MAPS_API_KEY is not configured.")

    def _headers(self, field_mask: str) -> dict[str, str]:
        return {
            "Content-Type": "application/json",
            "X-Goog-Api-Key": self.api_key,
            "X-Goog-FieldMask": field_mask,
        }

    async def nearby_search(
        self,
        latitude: float,
        longitude: float,
        radius_meters: float,
        included_types: list[str],
        max_result_count: int = 10,
    ) -> list[dict[str, Any]]:
        url = f"{self.base_url}/places:searchNearby"

        field_mask = ",".join(
            [
                "places.id",
                "places.displayName",
                "places.formattedAddress",
                "places.location",
                "places.rating",
                "places.userRatingCount",
                "places.primaryType",
                "places.types",
                "places.businessStatus",
                "places.currentOpeningHours",
                "places.evChargeOptions",
            ]
        )

        payload = {
            "includedTypes": included_types,
            "maxResultCount": max_result_count,
            "locationRestriction": {
                "circle": {
                    "center": {
                        "latitude": latitude,
                        "longitude": longitude,
                    },
                    "radius": radius_meters,
                }
            },
        }

        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.post(
                url,
                headers=self._headers(field_mask),
                json=payload,
            )
            response.raise_for_status()
            data = response.json()

        return data.get("places", [])

    async def text_search(
        self,
        text_query: str,
        latitude: float | None = None,
        longitude: float | None = None,
        radius_meters: float | None = None,
        max_result_count: int = 10,
    ) -> list[dict[str, Any]]:
        url = f"{self.base_url}/places:searchText"

        field_mask = ",".join(
            [
                "places.id",
                "places.displayName",
                "places.formattedAddress",
                "places.location",
                "places.rating",
                "places.userRatingCount",
                "places.primaryType",
                "places.types",
                "places.businessStatus",
                "places.currentOpeningHours",
                "places.evChargeOptions",
            ]
        )

        payload: dict[str, Any] = {
            "textQuery": text_query,
            "maxResultCount": max_result_count,
        }

        if latitude is not None and longitude is not None and radius_meters is not None:
            payload["locationBias"] = {
                "circle": {
                    "center": {
                        "latitude": latitude,
                        "longitude": longitude,
                    },
                    "radius": radius_meters,
                }
            }

        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.post(
                url,
                headers=self._headers(field_mask),
                json=payload,
            )
            response.raise_for_status()
            data = response.json()

        return data.get("places", [])

    async def place_details(self, place_id: str) -> dict[str, Any]:
        url = f"{self.base_url}/places/{place_id}"

        field_mask = ",".join(
            [
                "id",
                "displayName",
                "formattedAddress",
                "location",
                "rating",
                "userRatingCount",
                "primaryType",
                "types",
                "businessStatus",
                "currentOpeningHours",
                "internationalPhoneNumber",
                "websiteUri",
                "evChargeOptions",
            ]
        )

        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.get(
                url,
                headers=self._headers(field_mask),
            )
            response.raise_for_status()

        return response.json()
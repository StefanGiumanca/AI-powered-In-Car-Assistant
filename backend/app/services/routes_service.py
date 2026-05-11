import math
import os

import httpx
from fastapi import HTTPException, status


GOOGLE_ROUTES_URL = "https://routes.googleapis.com/directions/v2:computeRoutes"
GOOGLE_ROUTES_TIMEOUT_SECONDS = 10.0


def get_route(
    origin_lat: float,
    origin_lng: float,
    dest_lat: float,
    dest_lng: float,
    intermediates: list[dict[str, float]] | None = None,
) -> dict:
    api_key = os.getenv("GOOGLE_MAPS_API_KEY")
    if not api_key:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="GOOGLE_MAPS_API_KEY is not configured.",
        )

    payload = {
        "origin": {
            "location": {
                "latLng": {
                    "latitude": origin_lat,
                    "longitude": origin_lng,
                },
            },
        },
        "destination": {
            "location": {
                "latLng": {
                    "latitude": dest_lat,
                    "longitude": dest_lng,
                },
            },
        },
        "travelMode": "DRIVE",
        "routingPreference": "TRAFFIC_AWARE",
        "polylineQuality": "OVERVIEW",
        "polylineEncoding": "ENCODED_POLYLINE",
    }

    if intermediates:
        payload["intermediates"] = [
            {
                "location": {
                    "latLng": {
                        "latitude": stop["lat"],
                        "longitude": stop["lng"],
                    },
                },
            }
            for stop in intermediates
        ]

    headers = {
        "Content-Type": "application/json",
        "X-Goog-Api-Key": api_key,
        "X-Goog-FieldMask": (
            "routes.distanceMeters,"
            "routes.duration,"
            "routes.polyline.encodedPolyline"
        ),
    }

    try:
        response = httpx.post(
            GOOGLE_ROUTES_URL,
            json=payload,
            headers=headers,
            timeout=GOOGLE_ROUTES_TIMEOUT_SECONDS,
        )
    except httpx.TimeoutException:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API request timed out.",
        )
    except httpx.HTTPError:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API request failed.",
        )

    response_text = response.text

    if response.status_code in {401, 403} or (
        response.status_code == 400 and "api key" in response_text.lower()
    ):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Google Routes API key is invalid or not authorized.",
        )

    if response.status_code >= 400:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API returned an error.",
        )

    try:
        data = response.json()
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API returned an invalid response.",
        )

    routes = data.get("routes") or []
    if not routes:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API did not return a route.",
        )

    route = routes[0]
    distance_meters = route.get("distanceMeters")
    duration_value = route.get("duration")
    polyline = (route.get("polyline") or {}).get("encodedPolyline")

    if distance_meters is None or duration_value is None or not polyline:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API response is missing route data.",
        )

    duration_seconds = _parse_google_duration_seconds(duration_value)

    return {
        "distance_km": round(float(distance_meters) / 1000, 2),
        "duration_minutes": math.ceil(duration_seconds / 60),
        "polyline": polyline,
    }


def _parse_google_duration_seconds(duration_value: str) -> int:
    if not duration_value.endswith("s"):
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API returned an invalid duration.",
        )

    try:
        return int(float(duration_value[:-1]))
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Google Routes API returned an invalid duration.",
        )

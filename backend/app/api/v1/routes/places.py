from fastapi import APIRouter, Query

from app.services.google_places_client import GooglePlacesClient

router = APIRouter(prefix="/places", tags=["places"])


@router.get("/nearby")
async def test_nearby_places(
    lat: float = Query(...),
    lng: float = Query(...),
    place_type: str = Query("electric_vehicle_charging_station"),
    radius_meters: float = Query(5000),
):
    client = GooglePlacesClient()

    places = await client.nearby_search(
        latitude=lat,
        longitude=lng,
        radius_meters=radius_meters,
        included_types=[place_type],
        max_result_count=5,
    )

    return {
        "count": len(places),
        "places": places,
    }


@router.get("/search")
async def search_places(
    query: str = Query(..., min_length=2, max_length=200),
    lat: float | None = Query(default=None, ge=-90, le=90),
    lng: float | None = Query(default=None, ge=-180, le=180),
    radius_meters: float | None = Query(default=50000, gt=0, le=50000),
):
    client = GooglePlacesClient()

    places = await client.text_search(
        text_query=query,
        latitude=lat,
        longitude=lng,
        radius_meters=radius_meters if lat is not None and lng is not None else None,
        max_result_count=8,
    )

    return {
        "count": len(places),
        "places": places,
    }


@router.get("/{place_id}")
async def get_place_details(place_id: str):
    client = GooglePlacesClient()
    return await client.place_details(place_id)

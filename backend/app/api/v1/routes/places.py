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
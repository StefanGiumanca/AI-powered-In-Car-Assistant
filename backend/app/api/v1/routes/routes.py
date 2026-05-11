from fastapi import APIRouter

from app.schemas.routes import RoutePreviewRequest, RoutePreviewResponse
from app.services.routes_service import get_route


router = APIRouter(prefix="/routes", tags=["routes"])


@router.post("/preview", response_model=RoutePreviewResponse)
def preview_route(payload: RoutePreviewRequest) -> RoutePreviewResponse:
    route = get_route(
        origin_lat=payload.origin.lat,
        origin_lng=payload.origin.lng,
        dest_lat=payload.destination.lat,
        dest_lng=payload.destination.lng,
        intermediates=[
            {
                "lat": stop.lat,
                "lng": stop.lng,
            }
            for stop in payload.stops
        ],
    )

    return RoutePreviewResponse(**route)

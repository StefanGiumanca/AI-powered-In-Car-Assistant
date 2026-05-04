from pydantic import Field

from .common import APIModel, LabeledLocationDTO


class RouteDTO(APIModel):
    distance_km: float = Field(..., ge=0)
    duration_minutes: int = Field(..., ge=0)
    polyline: str = Field(..., min_length=1)


class RoutePreviewRequest(APIModel):
    origin: LabeledLocationDTO
    destination: LabeledLocationDTO


class RoutePreviewResponse(RouteDTO):
    pass

from pydantic import Field

from .common import APIModel, LabeledLocationDTO


class RouteDTO(APIModel):
    distance_km: float = Field(..., ge=0)
    duration_minutes: int = Field(..., ge=0)
    polyline: str = Field(..., min_length=1)


class RoutePreviewRequest(APIModel):
    origin: LabeledLocationDTO
    destination: LabeledLocationDTO
    stops: list[LabeledLocationDTO] = Field(default_factory=list, max_length=8)
    current_range: str | None = Field(default=None, max_length=100)
    route_preferences: str | None = Field(default=None, max_length=500)


class RoutePreviewResponse(RouteDTO):
    pass

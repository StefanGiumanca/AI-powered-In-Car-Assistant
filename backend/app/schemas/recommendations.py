from typing import Literal

from pydantic import Field

from .common import (
    APIModel,
    IdentifiedLocationDTO,
    NamedLocationDTO,
    RecommendationPriority,
)


# DTOs for recommendation cards, next-action responses, offers, and user events.
class RecommendationDTO(APIModel):
    recommendation_id: str = Field(..., min_length=1, max_length=100)
    type: Literal[
        "fuel_stop",
        "charging_stop",
        "service_stop",
        "rest_stop",
        "food_stop",
        "parking",
        "partner_offer",
    ]
    title: str = Field(..., min_length=1, max_length=200)
    reason: str = Field(..., min_length=1, max_length=600)
    priority: RecommendationPriority
    location: NamedLocationDTO
    detour_minutes: int = Field(..., ge=0)
    loyalty_points: int = Field(..., ge=0)


class OfferDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    title: str = Field(..., min_length=1, max_length=200)
    loyalty_points: int = Field(..., ge=0)


class NextActionRequest(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)


class NextActionResponse(APIModel):
    recommendation_id: str = Field(..., min_length=1, max_length=100)
    action: Literal[
        "STOP_FOR_FUEL",
        "STOP_FOR_CHARGE",
        "STOP_FOR_SERVICE",
        "TAKE_BREAK",
        "VIEW_OFFER",
        "CONTINUE_TRIP",
    ]
    title: str = Field(..., min_length=1, max_length=200)
    reason: str = Field(..., min_length=1, max_length=600)
    priority: RecommendationPriority
    confidence: float = Field(..., ge=0, le=1)
    detour_minutes: int = Field(..., ge=0)
    location: IdentifiedLocationDTO
    offer: OfferDTO


class RecommendationEventRequest(APIModel):
    trip_id: str = Field(..., min_length=1, max_length=100)
    recommendation_id: str = Field(..., min_length=1, max_length=100)
    event_type: Literal["accepted", "rejected", "dismissed", "viewed"]


class RecommendationEventResponse(APIModel):
    saved: bool

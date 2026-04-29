from fastapi import APIRouter

from app.schemas import (
    NextActionRequest,
    NextActionResponse,
    RecommendationEventRequest,
    RecommendationEventResponse,
)
from app.services.recommendation_service import (
    get_next_action,
    save_recommendation_event,
)


router = APIRouter(prefix="/recommendations", tags=["recommendations"])


@router.post("/next-action", response_model=NextActionResponse)
def next_action(payload: NextActionRequest) -> NextActionResponse:
    return get_next_action(payload)


@router.post("/events", response_model=RecommendationEventResponse)
def recommendation_event(
    payload: RecommendationEventRequest,
) -> RecommendationEventResponse:
    return save_recommendation_event(payload)

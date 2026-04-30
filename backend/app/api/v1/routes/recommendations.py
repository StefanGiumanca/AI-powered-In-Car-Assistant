from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db.database import get_db
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
def next_action(
    payload: NextActionRequest,
    db: Session = Depends(get_db),
) -> NextActionResponse:
    return get_next_action(payload, db)


@router.post("/events", response_model=RecommendationEventResponse)
def recommendation_event(
    payload: RecommendationEventRequest,
    db: Session = Depends(get_db),
) -> RecommendationEventResponse:
    return save_recommendation_event(payload, db)
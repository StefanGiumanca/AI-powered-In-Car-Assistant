from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.schemas.query_agent import RecommendationQueryRequest, RecommendationQueryResponse
from app.services.driver_query_recommendation_service import recommend_from_driver_query
from app.schemas.query_agent import AcceptRecommendationRequest
from app.services.accept_recommendation_service import accept_recommendation

router = APIRouter(prefix="/recommendations", tags=["recommendations"])

@router.post("/accept")
def accept_recommendation_endpoint(
    payload: AcceptRecommendationRequest,
    db: Session = Depends(get_db),
):
    return accept_recommendation(db=db, payload=payload)

@router.post("/driver-query", response_model=RecommendationQueryResponse)
async def recommend_from_query(
    payload: RecommendationQueryRequest,
    radius_meters: int | None = Query(None),
    db: Session = Depends(get_db),
):
    return await recommend_from_driver_query(
        db=db,
        payload=payload,
        radius_override_meters=radius_meters,
    )
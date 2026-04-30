from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.schemas import (
    TripStartRequest,
    TripStartResponse,
    TripUpdateRequest,
    TripUpdateResponse,
)
from app.services.trip_service import start_trip, update_trip


router = APIRouter(prefix="/trip", tags=["trip"])


@router.post(
    "/start",
    response_model=TripStartResponse,
    status_code=status.HTTP_201_CREATED,
)
def start_trip_endpoint(
    payload: TripStartRequest,
    db: Session = Depends(get_db),
) -> TripStartResponse:
    return start_trip(payload, db)


@router.post("/update", response_model=TripUpdateResponse)
def update_trip_endpoint(
    payload: TripUpdateRequest,
    db: Session = Depends(get_db),
) -> TripUpdateResponse:
    return update_trip(payload, db)
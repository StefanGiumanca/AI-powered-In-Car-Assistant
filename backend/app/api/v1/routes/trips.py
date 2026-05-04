from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db import models as dbm
from app.db.database import get_db
from app.schemas import (
    TripDetailResponse,
    TripStartRequest,
    TripStartResponse,
    TripUpdateRequest,
    TripUpdateResponse,
)
from app.services.trip_service import get_trip, start_trip, update_trip


router = APIRouter(tags=["trip"])


@router.post(
    "/trip/start",
    response_model=TripStartResponse,
    status_code=status.HTTP_201_CREATED,
)
def start_trip_endpoint(
    payload: TripStartRequest,
    db: Session = Depends(get_db),
    current_user: dbm.User = Depends(get_current_user),
) -> TripStartResponse:
    return start_trip(payload, current_user, db)


@router.get("/trips/{trip_id}", response_model=TripDetailResponse)
def get_trip_endpoint(
    trip_id: str,
    db: Session = Depends(get_db),
    current_user: dbm.User = Depends(get_current_user),
) -> TripDetailResponse:
    return get_trip(trip_id, current_user, db)


@router.post("/trip/update", response_model=TripUpdateResponse)
def update_trip_endpoint(
    payload: TripUpdateRequest,
    db: Session = Depends(get_db),
    current_user: dbm.User = Depends(get_current_user),
) -> TripUpdateResponse:
    return update_trip(payload, current_user, db)

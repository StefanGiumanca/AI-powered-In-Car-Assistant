from fastapi import APIRouter, status

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
def start_trip_endpoint(payload: TripStartRequest) -> TripStartResponse:
    return start_trip(payload)


@router.post("/update", response_model=TripUpdateResponse)
def update_trip_endpoint(payload: TripUpdateRequest) -> TripUpdateResponse:
    return update_trip(payload)

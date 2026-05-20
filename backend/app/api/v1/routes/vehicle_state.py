from typing import Annotated

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.db.models import User
from app.schemas import VehicleStateCreateRequest, VehicleStateResponse
from app.services.vehicle_state_service import create_vehicle_state_snapshot


router = APIRouter(prefix="/vehicle-state", tags=["vehicle-state"])


@router.post(
    "",
    response_model=VehicleStateResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_vehicle_state_endpoint(
    payload: VehicleStateCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> VehicleStateResponse:
    return create_vehicle_state_snapshot(
        db=db,
        current_user=current_user,
        payload=payload,
    )

from typing import Annotated

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.db.models import User
from app.schemas import VehicleCreateRequest, VehicleResponse
from app.services.vehicle_service import create_vehicle, delete_vehicle


router = APIRouter(prefix="/vehicles", tags=["vehicles"])


@router.post(
    "",
    response_model=VehicleResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_vehicle_endpoint(
    payload: VehicleCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> VehicleResponse:
    return create_vehicle(
        db=db,
        current_user=current_user,
        payload=payload,
    )


@router.delete(
    "/{vehicle_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_vehicle_endpoint(
    vehicle_id: str,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    delete_vehicle(
        db=db,
        current_user=current_user,
        vehicle_id=vehicle_id,
    )

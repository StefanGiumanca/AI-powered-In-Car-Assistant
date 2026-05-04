from typing import Annotated

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.db.models import User
from app.schemas import DriverProfileCreateRequest, DriverProfileResponse
from app.services.driver_profile_service import create_driver_profile


router = APIRouter(prefix="/driver-profiles", tags=["driver-profiles"])


@router.post(
    "",
    response_model=DriverProfileResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_driver_profile_endpoint(
    payload: DriverProfileCreateRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> DriverProfileResponse:
    return create_driver_profile(
        db=db,
        current_user=current_user,
        payload=payload,
    )

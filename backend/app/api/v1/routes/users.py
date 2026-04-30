from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db import models as dbm
from app.db.database import get_db
from app.db.models import User
from app.schemas import BootstrapResponse
from app.services.bootstrap_service import get_bootstrap_context


router = APIRouter(prefix="/me", tags=["me"])


@router.get("/bootstrap", response_model=BootstrapResponse)
def get_bootstrap(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> BootstrapResponse:
    return get_bootstrap_context(db=db, current_user=current_user)

@router.get("/users")
def list_users(db: Session = Depends(get_db)):
    users = db.query(dbm.User).all()

    return [
        {
            "id": str(user.id),
            "email": user.email,
            "full_name": user.full_name,
        }
        for user in users
    ]

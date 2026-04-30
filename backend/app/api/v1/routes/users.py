from uuid import UUID

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.db.database import get_db
from app.schemas import BootstrapResponse
from app.services.bootstrap_service import get_bootstrap_context


router = APIRouter(prefix="/me", tags=["me"])


@router.get("/bootstrap", response_model=BootstrapResponse)
def get_bootstrap(
    user_id: UUID | None = Query(None),
    db: Session = Depends(get_db),
) -> BootstrapResponse:
    return get_bootstrap_context(db=db, user_id=user_id)

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
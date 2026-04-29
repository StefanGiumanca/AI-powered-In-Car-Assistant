from fastapi import APIRouter

from app.schemas import BootstrapResponse
from app.services.bootstrap_service import get_bootstrap_context


router = APIRouter(prefix="/me", tags=["me"])


@router.get("/bootstrap", response_model=BootstrapResponse)
def get_bootstrap() -> BootstrapResponse:
    return get_bootstrap_context()

from fastapi import FastAPI

from app.api.v1.routes import recommendations, trips, vehicle_state
from app.core.config import settings


app = FastAPI(title=settings.app_name)

app.include_router(trips.router, prefix="/api/v1/trips", tags=["trips"])
app.include_router(
    vehicle_state.router,
    prefix="/api/v1/vehicle-state",
    tags=["vehicle-state"],
)
app.include_router(
    recommendations.router,
    prefix="/api/v1/recommendations",
    tags=["recommendations"],
)

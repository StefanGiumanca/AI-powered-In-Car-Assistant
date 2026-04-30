from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.routes.auth import router as auth_router
from app.api.v1.routes.driver_profiles import router as driver_profiles_router
from app.api.v1.routes.recommendations import router as recommendations_router
from app.api.v1.routes.trips import router as trips_router
from app.api.v1.routes.users import router as users_router
from app.api.v1.routes.vehicle_state import router as vehicle_state_router
from app.api.v1.routes.vehicles import router as vehicles_router


app = FastAPI(
    title="DavaRoutes API",
    description="Backend API for AI-powered in-car assistant.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://localhost:3000",
        "http://localhost:8501",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(users_router)
app.include_router(driver_profiles_router)
app.include_router(trips_router)
app.include_router(recommendations_router)
app.include_router(vehicle_state_router)
app.include_router(vehicles_router)



@app.get("/")
def root():
    return {"message": "DavaRoutes backend is running"}


@app.get("/health")
def health():
    return {"status": "ok"}

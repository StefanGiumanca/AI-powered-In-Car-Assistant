from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.routes.recommendations import router as recommendations_router
from app.api.v1.routes.trips import router as trips_router
from app.api.v1.routes.users import router as users_router

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

app.include_router(users_router)
app.include_router(trips_router)
app.include_router(recommendations_router)


@app.get("/")
def root():
    return {"message": "DavaRoutes backend is running"}


@app.get("/health")
def health():
    return {"status": "ok"}
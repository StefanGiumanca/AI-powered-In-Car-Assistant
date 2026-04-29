from fastapi import FastAPI
# Adaugat de mine
from pydantic import BaseModel

from app.api.v1.routes.recommendations import router as recommendations_router
from app.api.v1.routes.trips import router as trips_router
from app.api.v1.routes.users import router as users_router

app = FastAPI()

app.include_router(recommendations_router)
app.include_router(trips_router)
app.include_router(users_router)

# Adaugat de mine
class Trip(BaseModel):
    user_id: str
    vehicle_id: str
    driver_profile_id: str | None = None

    origin_label: str
    origin_lat: float
    origin_lng: float

    destination_label: str
    destination_lat: float
    destination_lng: float

    departure_time: str
    requested_mode: str

@app.post("/trips")
def create_trip(trip: Trip):
    print("Received trip:", trip)
    return {
        "message": "Trip received",
        "trip": trip
    }

# Pana aici am adaugat eu, Paul. Daca vrei, Vlad, sterge si fa altcumva

@app.get("/")
def root():
    return {
        "message": "DavaRoutes backend is running"
    }


@app.get("/health")
def health():
    return {
        "status": "ok"
    }


@app.get("/recommendations")
def recommendations():
    return {
        "items": [
            {
                "name": "OMV Charging Station",
                "type": "charging",
                "distance_km": 42
            },
            {
                "name": "McDonald's Drive-Thru",
                "type": "food",
                "distance_km": 45
            }
        ]
    }

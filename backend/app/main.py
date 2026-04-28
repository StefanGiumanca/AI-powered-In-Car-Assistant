<<<<<<<< f8193069009012b070d8edb398d820b8b8207c6d:backend/app/main.py
from fastapi import FastAPI

app = FastAPI()


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
========
from app.main import app
>>>>>>>> 9324f52982ad4809b52bb67fe949438e52e7079c:backend/main.py
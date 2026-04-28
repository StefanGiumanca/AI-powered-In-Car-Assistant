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
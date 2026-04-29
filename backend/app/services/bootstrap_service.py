from app.schemas import BootstrapResponse


def get_bootstrap_context() -> BootstrapResponse:
    return BootstrapResponse(
        user={
            "id": "user_1",
            "full_name": "Demo User",
        },
        vehicle={
            "id": "vehicle_1",
            "model": "Dacia Duster",
            "powertrain": "ICE",
            "fuel_tank_liters": 50,
            "consumption_l_per_100km": 7.2,
        },
        driver_profile={
            "id": "profile_1",
            "profile_type": "balanced",
            "avoid_tolls": False,
            "max_detour_minutes": 10,
        },
    )

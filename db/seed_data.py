from app.db.database import SessionLocal
from app.db import models as dbm


def seed() -> None:
    db = SessionLocal()

    existing = db.query(dbm.User).filter(dbm.User.email == "demo@davaroutes.com").first()
    if existing:
        print("Seed already exists.")
        db.close()
        return

    user = dbm.User(
        email="demo@davaroutes.com",
        password_hash="demo_password_hash",
        full_name="Demo User",
    )

    db.add(user)
    db.flush()

    vehicle = dbm.Vehicle(
        user_id=user.id,
        model="Dacia Duster",
        year=2024,
        powertrain="ICE",
        fuel_tank_liters=50,
        consumption_l_per_100km=7.2,
        service_interval_km=15000,
    )

    profile = dbm.DriverProfile(
        user_id=user.id,
        profile_name="Balanced Profile",
        profile_type="balanced",
        preferred_amenities=["restaurant", "restroom"],
        preferred_brands=["OMV", "MOL"],
        avoid_tolls=False,
        avoid_highways=False,
        max_detour_minutes=10,
        break_frequency_minutes=120,
    )

    db.add(vehicle)
    db.add(profile)

    db.commit()
    db.close()

    print("Seed data inserted.")


if __name__ == "__main__":
    seed()
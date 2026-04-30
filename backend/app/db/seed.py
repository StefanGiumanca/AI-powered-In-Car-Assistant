from app.db.database import SessionLocal
from app.db import models as dbm


def get_or_create_user(db, email: str, full_name: str, password_hash: str):
    user = db.query(dbm.User).filter(dbm.User.email == email).first()

    if user:
        print(f"User already exists: {email}")
        return user

    user = dbm.User(
        email=email,
        password_hash=password_hash,
        full_name=full_name,
    )

    db.add(user)
    db.flush()

    print(f"Created user: {email}")
    return user


def get_or_create_vehicle(
    db,
    user_id,
    model: str,
    year: int,
    powertrain: str,
    vin: str | None = None,
    fuel_tank_liters: float | None = None,
    consumption_l_per_100km: float | None = None,
    battery_capacity_kwh: float | None = None,
    consumption_kwh_per_100km: float | None = None,
    connector_type: str | None = None,
    service_interval_km: int | None = None,
):
    query = db.query(dbm.Vehicle).filter(
        dbm.Vehicle.user_id == user_id,
        dbm.Vehicle.model == model,
        dbm.Vehicle.year == year,
    )

    vehicle = query.first()

    if vehicle:
        print(f"Vehicle already exists: {model} {year}")
        return vehicle

    vehicle = dbm.Vehicle(
        user_id=user_id,
        vin=vin,
        model=model,
        year=year,
        powertrain=powertrain,
        fuel_tank_liters=fuel_tank_liters,
        consumption_l_per_100km=consumption_l_per_100km,
        battery_capacity_kwh=battery_capacity_kwh,
        consumption_kwh_per_100km=consumption_kwh_per_100km,
        connector_type=connector_type,
        service_interval_km=service_interval_km,
    )

    db.add(vehicle)
    db.flush()

    print(f"Created vehicle: {model} {year}")
    return vehicle


def get_or_create_driver_profile(
    db,
    user_id,
    profile_name: str,
    profile_type: str,
    preferred_amenities: list[str],
    preferred_brands: list[str],
    avoid_tolls: bool,
    avoid_highways: bool,
    max_detour_minutes: int,
    break_frequency_minutes: int,
):
    profile = db.query(dbm.DriverProfile).filter(
        dbm.DriverProfile.user_id == user_id,
        dbm.DriverProfile.profile_name == profile_name,
    ).first()

    if profile:
        print(f"Driver profile already exists: {profile_name}")
        return profile

    profile = dbm.DriverProfile(
        user_id=user_id,
        profile_name=profile_name,
        profile_type=profile_type,
        preferred_amenities=preferred_amenities,
        preferred_brands=preferred_brands,
        avoid_tolls=avoid_tolls,
        avoid_highways=avoid_highways,
        max_detour_minutes=max_detour_minutes,
        break_frequency_minutes=break_frequency_minutes,
    )

    db.add(profile)
    db.flush()

    print(f"Created driver profile: {profile_name}")
    return profile


def seed() -> None:
    db = SessionLocal()

    try:
        # User 1: ICE vehicle
        user_1 = get_or_create_user(
            db=db,
            email="demo@davaroutes.com",
            full_name="Demo User",
            password_hash="demo_password_hash",
        )

        get_or_create_vehicle(
            db=db,
            user_id=user_1.id,
            model="Dacia Logan",
            year=2024,
            powertrain="ICE",
            fuel_tank_liters=50,
            consumption_l_per_100km=7.2,
            service_interval_km=15000,
        )

        get_or_create_driver_profile(
            db=db,
            user_id=user_1.id,
            profile_name="Balanced Profile",
            profile_type="balanced",
            preferred_amenities=["restaurant", "restroom"],
            preferred_brands=["OMV", "MOL"],
            avoid_tolls=False,
            avoid_highways=False,
            max_detour_minutes=10,
            break_frequency_minutes=120,
        )

        # User 2: EV vehicle
        user_2 = get_or_create_user(
            db=db,
            email="ev.driver@davaroutes.com",
            full_name="EV Demo Driver",
            password_hash="demo_password_hash",
        )

        get_or_create_vehicle(
            db=db,
            user_id=user_2.id,
            model="Tesla Model 3",
            year=2024,
            powertrain="EV",
            connector_type="CCS2",
            battery_capacity_kwh=75,
            consumption_kwh_per_100km=16.5,
            service_interval_km=20000,
        )

        get_or_create_driver_profile(
            db=db,
            user_id=user_2.id,
            profile_name="Family EV Profile",
            profile_type="family",
            preferred_amenities=["restaurant", "restroom", "playground"],
            preferred_brands=["Ionity", "Kaufland", "Starbucks"],
            avoid_tolls=False,
            avoid_highways=False,
            max_detour_minutes=15,
            break_frequency_minutes=90,
        )

        db.commit()
        print("Seed data inserted successfully.")

    except Exception as exc:
        db.rollback()
        print(f"Seed failed: {exc}")
        raise

    finally:
        db.close()


if __name__ == "__main__":
    seed()
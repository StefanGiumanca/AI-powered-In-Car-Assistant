from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas import BootstrapResponse


def get_bootstrap_context(
    db: Session,
    user_id: UUID | None = None,
) -> BootstrapResponse:
    if user_id is not None:
        user = db.query(dbm.User).filter(dbm.User.id == user_id).first()
    else:
        user = db.query(dbm.User).first()

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No users found. Seed the database first.",
        )

    vehicle = (
        db.query(dbm.Vehicle)
        .filter(dbm.Vehicle.user_id == user.id)
        .first()
    )

    if vehicle is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No vehicle found for user.",
        )

    driver_profile = (
        db.query(dbm.DriverProfile)
        .filter(dbm.DriverProfile.user_id == user.id)
        .first()
    )

    if driver_profile is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No driver profile found for user.",
        )

    return BootstrapResponse(
        user={
            "id": str(user.id),
            "full_name": user.full_name or "Unknown User",
        },
        vehicle={
            "id": str(vehicle.id),
            "model": vehicle.model or "Unknown Vehicle",
            "powertrain": vehicle.powertrain,
            "fuel_tank_liters": (
                float(vehicle.fuel_tank_liters)
                if vehicle.fuel_tank_liters is not None
                else None
            ),
            "consumption_l_per_100km": (
                float(vehicle.consumption_l_per_100km)
                if vehicle.consumption_l_per_100km is not None
                else None
            ),
            "battery_capacity_kwh": (
                float(vehicle.battery_capacity_kwh)
                if vehicle.battery_capacity_kwh is not None
                else None
            ),
            "consumption_kwh_per_100km": (
                float(vehicle.consumption_kwh_per_100km)
                if vehicle.consumption_kwh_per_100km is not None
                else None
            ),
            "connector_type": vehicle.connector_type,
        },
        driver_profile={
            "id": str(driver_profile.id),
            "profile_type": driver_profile.profile_type,
            "avoid_tolls": driver_profile.avoid_tolls,
            "max_detour_minutes": driver_profile.max_detour_minutes or 10,
        },
    )
from decimal import Decimal

from sqlalchemy.orm import Session

from app.db import models as dbm
from app.db.models import User
from app.schemas import BootstrapResponse


def _to_float(value: Decimal | None) -> float | None:
    if value is None:
        return None

    return float(value)


def get_bootstrap_context(
    db: Session,
    current_user: User,
) -> BootstrapResponse:
    vehicles = (
        db.query(dbm.Vehicle)
        .filter(dbm.Vehicle.user_id == current_user.id)
        .all()
    )

    driver_profiles = (
        db.query(dbm.DriverProfile)
        .filter(dbm.DriverProfile.user_id == current_user.id)
        .all()
    )

    latest_vehicle_state = (
        db.query(dbm.VehicleStateSnapshot)
        .join(dbm.Vehicle, dbm.Vehicle.id == dbm.VehicleStateSnapshot.vehicle_id)
        .filter(dbm.Vehicle.user_id == current_user.id)
        .order_by(dbm.VehicleStateSnapshot.captured_at.desc())
        .first()
    )

    return BootstrapResponse(
        user={
            "id": str(current_user.id),
            "email": current_user.email,
            "full_name": current_user.full_name,
        },
        vehicles=[
            {
                "id": str(vehicle.id),
                "model": vehicle.model,
                "year": vehicle.year,
                "powertrain": vehicle.powertrain,
                "fuel_tank_liters": _to_float(vehicle.fuel_tank_liters),
                "consumption_l_per_100km": _to_float(
                    vehicle.consumption_l_per_100km
                ),
                "battery_capacity_kwh": _to_float(vehicle.battery_capacity_kwh),
                "consumption_kwh_per_100km": _to_float(
                    vehicle.consumption_kwh_per_100km
                ),
                "connector_type": vehicle.connector_type,
            }
            for vehicle in vehicles
        ],
        driver_profiles=[
            {
                "id": str(driver_profile.id),
                "profile_name": driver_profile.profile_name,
                "profile_type": driver_profile.profile_type,
                "avoid_tolls": driver_profile.avoid_tolls,
                "avoid_highways": driver_profile.avoid_highways,
                "max_detour_minutes": driver_profile.max_detour_minutes or 10,
                "break_frequency_minutes": (
                    driver_profile.break_frequency_minutes or 120
                ),
                "preferred_brands": driver_profile.preferred_brands or [],
                "preferred_amenities": driver_profile.preferred_amenities or [],
            }
            for driver_profile in driver_profiles
        ],
        latest_vehicle_state=(
            {
                "id": str(latest_vehicle_state.id),
                "vehicle_id": str(latest_vehicle_state.vehicle_id),
                "fuel_level_percent": _to_float(
                    latest_vehicle_state.fuel_level_percent
                ),
                "battery_soc_percent": _to_float(
                    latest_vehicle_state.battery_soc_percent
                ),
                "estimated_range_km": _to_float(
                    latest_vehicle_state.estimated_range_km
                ),
                "odometer_km": _to_float(latest_vehicle_state.odometer_km),
                "tire_pressure_status": latest_vehicle_state.tire_pressure_status,
                "captured_at": latest_vehicle_state.captured_at,
            }
            if latest_vehicle_state is not None
            else None
        ),
    )

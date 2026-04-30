from decimal import Decimal

from sqlalchemy.orm import Session

from app.db.models import User, Vehicle
from app.schemas import VehicleCreateRequest, VehicleResponse


def _to_decimal(value: float | None) -> Decimal | None:
    if value is None:
        return None

    return Decimal(str(value))


def vehicle_to_response(vehicle: Vehicle) -> VehicleResponse:
    return VehicleResponse(
        id=str(vehicle.id),
        vin=vehicle.vin,
        model=vehicle.model or "",
        year=vehicle.year,
        powertrain=vehicle.powertrain,
        connector_type=vehicle.connector_type,
        battery_capacity_kwh=(
            float(vehicle.battery_capacity_kwh)
            if vehicle.battery_capacity_kwh is not None
            else None
        ),
        fuel_tank_liters=(
            float(vehicle.fuel_tank_liters)
            if vehicle.fuel_tank_liters is not None
            else None
        ),
        consumption_kwh_per_100km=(
            float(vehicle.consumption_kwh_per_100km)
            if vehicle.consumption_kwh_per_100km is not None
            else None
        ),
        consumption_l_per_100km=(
            float(vehicle.consumption_l_per_100km)
            if vehicle.consumption_l_per_100km is not None
            else None
        ),
        service_interval_km=vehicle.service_interval_km,
        service_interval_months=vehicle.service_interval_months,
    )


def create_vehicle(
    db: Session,
    current_user: User,
    payload: VehicleCreateRequest,
) -> VehicleResponse:
    vehicle = Vehicle(
        user_id=current_user.id,
        vin=payload.vin,
        model=payload.model,
        year=payload.year,
        powertrain=payload.powertrain,
        connector_type=payload.connector_type,
        battery_capacity_kwh=_to_decimal(payload.battery_capacity_kwh),
        fuel_tank_liters=_to_decimal(payload.fuel_tank_liters),
        consumption_kwh_per_100km=_to_decimal(payload.consumption_kwh_per_100km),
        consumption_l_per_100km=_to_decimal(payload.consumption_l_per_100km),
        service_interval_km=payload.service_interval_km,
        service_interval_months=payload.service_interval_months,
    )

    db.add(vehicle)
    db.commit()
    db.refresh(vehicle)

    return vehicle_to_response(vehicle)

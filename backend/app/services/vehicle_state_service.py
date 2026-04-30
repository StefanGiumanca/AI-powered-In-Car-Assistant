from decimal import Decimal
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.db.models import User, Vehicle, VehicleStateSnapshot
from app.schemas import VehicleStateCreateRequest, VehicleStateResponse


def _to_decimal(value: float | None) -> Decimal | None:
    if value is None:
        return None

    return Decimal(str(value))


def _parse_uuid(value: str) -> UUID:
    try:
        return UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="vehicle_id must be a valid UUID.",
        ) from None


def vehicle_state_to_response(
    snapshot: VehicleStateSnapshot,
) -> VehicleStateResponse:
    return VehicleStateResponse(
        id=str(snapshot.id),
        vehicle_id=str(snapshot.vehicle_id),
        fuel_level_percent=(
            float(snapshot.fuel_level_percent)
            if snapshot.fuel_level_percent is not None
            else None
        ),
        battery_soc_percent=(
            float(snapshot.battery_soc_percent)
            if snapshot.battery_soc_percent is not None
            else None
        ),
        estimated_range_km=(
            float(snapshot.estimated_range_km)
            if snapshot.estimated_range_km is not None
            else None
        ),
        odometer_km=(
            float(snapshot.odometer_km)
            if snapshot.odometer_km is not None
            else None
        ),
        tire_pressure_status=snapshot.tire_pressure_status,
        captured_at=snapshot.captured_at,
    )


def create_vehicle_state_snapshot(
    db: Session,
    current_user: User,
    payload: VehicleStateCreateRequest,
) -> VehicleStateResponse:
    vehicle_id = _parse_uuid(payload.vehicle_id)

    vehicle = (
        db.query(Vehicle)
        .filter(
            Vehicle.id == vehicle_id,
            Vehicle.user_id == current_user.id,
        )
        .first()
    )

    if vehicle is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Vehicle not found.",
        )

    snapshot = VehicleStateSnapshot(
        vehicle_id=vehicle.id,
        fuel_level_percent=_to_decimal(payload.fuel_level_percent),
        battery_soc_percent=_to_decimal(payload.battery_soc_percent),
        estimated_range_km=_to_decimal(payload.estimated_range_km),
        odometer_km=_to_decimal(payload.odometer_km),
        tire_pressure_status=payload.tire_pressure_status,
    )

    db.add(snapshot)
    db.commit()
    db.refresh(snapshot)

    return vehicle_state_to_response(snapshot)

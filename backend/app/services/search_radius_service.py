from app.db import models as dbm
from app.schemas.query_agent import QueryInterpretation


DEFAULT_RADIUS_METERS = 5000
EXPANDED_RADIUS_METERS = 10000
MAX_RADIUS_METERS = 50000


def determine_initial_radius(
    interpretation: QueryInterpretation,
    vehicle: dbm.Vehicle,
    vehicle_state: dbm.VehicleStateSnapshot | None,
) -> int:
    if vehicle_state is None:
        return interpretation.radius_meters or DEFAULT_RADIUS_METERS

    # EV urgent case
    if vehicle.powertrain == "EV" and vehicle_state.battery_soc_percent is not None:
        soc = float(vehicle_state.battery_soc_percent)

        if soc <= 10:
            return min(int(float(vehicle_state.estimated_range_km or 20) * 1000), MAX_RADIUS_METERS)

        if soc <= 25:
            return min(15000, MAX_RADIUS_METERS)

    # ICE / Hybrid urgent case
    if vehicle.powertrain in {"ICE", "HYBRID"} and vehicle_state.fuel_level_percent is not None:
        fuel = float(vehicle_state.fuel_level_percent)

        if fuel <= 10:
            return min(int(float(vehicle_state.estimated_range_km or 20) * 1000), MAX_RADIUS_METERS)

        if fuel <= 25:
            return min(15000, MAX_RADIUS_METERS)

    return min(interpretation.radius_meters or DEFAULT_RADIUS_METERS, DEFAULT_RADIUS_METERS)


def get_next_radius(current_radius_meters: int) -> int | None:
    if current_radius_meters < EXPANDED_RADIUS_METERS:
        return EXPANDED_RADIUS_METERS

    if current_radius_meters < 20000:
        return 20000

    return None
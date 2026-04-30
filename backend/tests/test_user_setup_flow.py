import asyncio

import pytest
from sqlalchemy import create_engine
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.compiler import compiles
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.security import create_access_token, hash_password
from app.db.database import get_db
from app.db.models import DriverProfile, User, Vehicle, VehicleStateSnapshot
from app.main import app
from tests.asgi_client import request_app


@compiles(JSONB, "sqlite")
def compile_jsonb_sqlite(_type, compiler, **kwargs):
    return "JSON"


@pytest.fixture()
def db_session_factory():
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    SessionLocal = sessionmaker(
        autocommit=False,
        autoflush=False,
        bind=engine,
    )

    for table in (
        User.__table__,
        Vehicle.__table__,
        DriverProfile.__table__,
        VehicleStateSnapshot.__table__,
    ):
        table.create(bind=engine, checkfirst=True)

    def override_get_db():
        db = SessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db

    try:
        yield SessionLocal
    finally:
        app.dependency_overrides.pop(get_db, None)
        for table in (
            VehicleStateSnapshot.__table__,
            DriverProfile.__table__,
            Vehicle.__table__,
            User.__table__,
        ):
            table.drop(bind=engine, checkfirst=True)
        engine.dispose()


def create_user(db_session_factory, email: str = "demo@test.com") -> User:
    db = db_session_factory()
    try:
        user = User(
            email=email,
            password_hash=hash_password("test1234"),
            full_name="Demo User",
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        db.expunge(user)
        return user
    finally:
        db.close()


def auth_headers(user: User) -> dict[str, str]:
    token = create_access_token(str(user.id))
    return {"Authorization": f"Bearer {token}"}


def test_bootstrap_unauthorized_returns_401(db_session_factory):
    status, body = asyncio.run(request_app("GET", "/me/bootstrap"))

    assert status == 401
    assert body["detail"] == "Not authenticated."


def test_bootstrap_authorized_user_without_setup_returns_empty_data(
    db_session_factory,
):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app("GET", "/me/bootstrap", headers=auth_headers(user))
    )

    assert status == 200
    assert body["user"]["id"] == str(user.id)
    assert body["user"]["email"] == "demo@test.com"
    assert body["vehicles"] == []
    assert body["driver_profiles"] == []
    assert body["latest_vehicle_state"] is None


def test_bootstrap_authorized_user_with_setup_returns_all_data(
    db_session_factory,
):
    user = create_user(db_session_factory)
    db = db_session_factory()
    try:
        vehicle = Vehicle(
            user_id=user.id,
            model="Dacia Duster",
            year=2021,
            powertrain="ICE",
            fuel_tank_liters=50,
            consumption_l_per_100km=7.2,
        )
        db.add(vehicle)
        db.flush()

        profile = DriverProfile(
            user_id=user.id,
            profile_name="Default",
            profile_type="balanced",
            preferred_amenities=["coffee", "parking"],
            preferred_brands=["OMV", "MOL"],
            avoid_tolls=False,
            avoid_highways=False,
            max_detour_minutes=10,
            break_frequency_minutes=120,
        )
        db.add(profile)
        db.flush()

        snapshot = VehicleStateSnapshot(
            vehicle_id=vehicle.id,
            fuel_level_percent=40,
            battery_soc_percent=None,
            estimated_range_km=280,
            odometer_km=84200,
            tire_pressure_status="ok",
        )
        db.add(snapshot)
        db.commit()
        db.refresh(vehicle)
        db.refresh(profile)
        db.refresh(snapshot)

        vehicle_id = str(vehicle.id)
        profile_id = str(profile.id)
        snapshot_id = str(snapshot.id)
    finally:
        db.close()

    status, body = asyncio.run(
        request_app("GET", "/me/bootstrap", headers=auth_headers(user))
    )

    assert status == 200
    assert body["vehicles"][0]["id"] == vehicle_id
    assert body["vehicles"][0]["model"] == "Dacia Duster"
    assert body["driver_profiles"][0]["id"] == profile_id
    assert body["driver_profiles"][0]["preferred_brands"] == ["OMV", "MOL"]
    assert body["latest_vehicle_state"]["id"] == snapshot_id
    assert body["latest_vehicle_state"]["vehicle_id"] == vehicle_id
    assert body["latest_vehicle_state"]["fuel_level_percent"] == 40.0


def test_create_vehicle_success(db_session_factory):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicles",
            {
                "vin": None,
                "model": "Dacia Duster",
                "year": 2021,
                "powertrain": "ICE",
                "connector_type": None,
                "battery_capacity_kwh": None,
                "fuel_tank_liters": 50,
                "consumption_kwh_per_100km": None,
                "consumption_l_per_100km": 7.2,
                "service_interval_km": 15000,
                "service_interval_months": 12,
            },
            headers=auth_headers(user),
        )
    )

    assert status == 201
    assert body["model"] == "Dacia Duster"
    assert body["powertrain"] == "ICE"
    assert body["fuel_tank_liters"] == 50.0


def test_create_vehicle_unauthorized_returns_401(db_session_factory):
    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicles",
            {
                "model": "Dacia Duster",
                "powertrain": "ICE",
            },
        )
    )

    assert status == 401
    assert body["detail"] == "Not authenticated."


def test_create_vehicle_invalid_powertrain_returns_422(db_session_factory):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicles",
            {
                "model": "Dacia Duster",
                "powertrain": "DIESEL",
            },
            headers=auth_headers(user),
        )
    )

    assert status == 422
    assert "detail" in body


def test_create_driver_profile_success(db_session_factory):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/driver-profiles",
            {
                "profile_name": "Default",
                "profile_type": "balanced",
                "preferred_amenities": ["coffee", "parking"],
                "preferred_brands": ["OMV", "MOL"],
                "avoid_tolls": False,
                "avoid_highways": False,
                "max_detour_minutes": 10,
                "break_frequency_minutes": 120,
            },
            headers=auth_headers(user),
        )
    )

    assert status == 201
    assert body["profile_name"] == "Default"
    assert body["profile_type"] == "balanced"
    assert body["preferred_amenities"] == ["coffee", "parking"]


def test_create_driver_profile_invalid_profile_type_returns_422(
    db_session_factory,
):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/driver-profiles",
            {
                "profile_name": "Default",
                "profile_type": "aggressive",
            },
            headers=auth_headers(user),
        )
    )

    assert status == 422
    assert "detail" in body


def test_create_driver_profile_defaults_are_applied(db_session_factory):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/driver-profiles",
            {
                "profile_name": "Default",
            },
            headers=auth_headers(user),
        )
    )

    assert status == 201
    assert body["profile_type"] == "balanced"
    assert body["avoid_tolls"] is False
    assert body["avoid_highways"] is False
    assert body["max_detour_minutes"] == 10
    assert body["break_frequency_minutes"] == 120


def test_create_vehicle_state_success(db_session_factory):
    user = create_user(db_session_factory)
    db = db_session_factory()
    try:
        vehicle = Vehicle(
            user_id=user.id,
            model="Dacia Duster",
            year=2021,
            powertrain="ICE",
        )
        db.add(vehicle)
        db.commit()
        db.refresh(vehicle)
        vehicle_id = str(vehicle.id)
    finally:
        db.close()

    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicle-state",
            {
                "vehicle_id": vehicle_id,
                "fuel_level_percent": 40,
                "battery_soc_percent": None,
                "estimated_range_km": 280,
                "odometer_km": 84200,
                "tire_pressure_status": "ok",
            },
            headers=auth_headers(user),
        )
    )

    assert status == 201
    assert body["vehicle_id"] == vehicle_id
    assert body["fuel_level_percent"] == 40.0
    assert body["captured_at"] is not None


def test_create_vehicle_state_unauthorized_returns_401(db_session_factory):
    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicle-state",
            {
                "vehicle_id": "00000000-0000-0000-0000-000000000000",
                "fuel_level_percent": 40,
                "tire_pressure_status": "ok",
            },
        )
    )

    assert status == 401
    assert body["detail"] == "Not authenticated."


def test_create_vehicle_state_for_another_users_vehicle_returns_404(
    db_session_factory,
):
    owner = create_user(db_session_factory, email="owner@test.com")
    other_user = create_user(db_session_factory, email="other@test.com")
    db = db_session_factory()
    try:
        vehicle = Vehicle(
            user_id=owner.id,
            model="Dacia Duster",
            year=2021,
            powertrain="ICE",
        )
        db.add(vehicle)
        db.commit()
        db.refresh(vehicle)
        vehicle_id = str(vehicle.id)
    finally:
        db.close()

    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicle-state",
            {
                "vehicle_id": vehicle_id,
                "fuel_level_percent": 40,
                "tire_pressure_status": "ok",
            },
            headers=auth_headers(other_user),
        )
    )

    assert status == 404
    assert body["detail"] == "Vehicle not found."


def test_create_vehicle_state_invalid_fuel_level_returns_422(
    db_session_factory,
):
    user = create_user(db_session_factory)
    db = db_session_factory()
    try:
        vehicle = Vehicle(
            user_id=user.id,
            model="Dacia Duster",
            year=2021,
            powertrain="ICE",
        )
        db.add(vehicle)
        db.commit()
        db.refresh(vehicle)
        vehicle_id = str(vehicle.id)
    finally:
        db.close()

    status, body = asyncio.run(
        request_app(
            "POST",
            "/vehicle-state",
            {
                "vehicle_id": vehicle_id,
                "fuel_level_percent": 140,
                "tire_pressure_status": "ok",
            },
            headers=auth_headers(user),
        )
    )

    assert status == 422
    assert "detail" in body

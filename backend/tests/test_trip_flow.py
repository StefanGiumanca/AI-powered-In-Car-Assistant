import asyncio
from uuid import UUID

import pytest
from sqlalchemy import create_engine
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.compiler import compiles
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.security import create_access_token, hash_password
from app.db.database import get_db
from app.db.models import (
    DriverProfile,
    Recommendation,
    RecommendationItem,
    RouteOption,
    Trip,
    User,
    Vehicle,
    VehicleStateSnapshot,
)
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
        Trip.__table__,
        RouteOption.__table__,
        Recommendation.__table__,
        RecommendationItem.__table__,
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
            RecommendationItem.__table__,
            Recommendation.__table__,
            RouteOption.__table__,
            Trip.__table__,
            VehicleStateSnapshot.__table__,
            DriverProfile.__table__,
            Vehicle.__table__,
            User.__table__,
        ):
            table.drop(bind=engine, checkfirst=True)
        engine.dispose()


def auth_headers(user: User) -> dict[str, str]:
    token = create_access_token(str(user.id))
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(autouse=True)
def google_route_sample(monkeypatch):
    route = {
        "distance_km": 168.42,
        "duration_minutes": 156,
        "polyline": "encoded_polyline",
    }

    monkeypatch.setattr("app.services.trip_service.get_route", lambda **_: route)
    monkeypatch.setattr("app.api.v1.routes.routes.get_route", lambda **_: route)
    return route


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


def create_setup(
    db_session_factory,
    user: User,
    fuel_level_percent: float | None = 40,
    battery_soc_percent: float | None = None,
    tire_pressure_status: str = "ok",
) -> tuple[str, str]:
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
            fuel_level_percent=fuel_level_percent,
            battery_soc_percent=battery_soc_percent,
            estimated_range_km=280,
            odometer_km=84200,
            tire_pressure_status=tire_pressure_status,
        )
        db.add(snapshot)
        db.commit()
        db.refresh(vehicle)
        db.refresh(profile)
        return str(vehicle.id), str(profile.id)
    finally:
        db.close()


def trip_start_payload(vehicle_id: str, profile_id: str) -> dict:
    return {
        "vehicle_id": vehicle_id,
        "driver_profile_id": profile_id,
        "origin": {
            "label": "Current location",
            "lat": 44.4268,
            "lng": 26.1025,
        },
        "destination": {
            "label": "Brasov",
            "lat": 45.6579,
            "lng": 25.6012,
        },
        "requested_mode": "balanced",
    }


def start_trip(db_session_factory, user: User, vehicle_id: str, profile_id: str) -> dict:
    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/start",
            trip_start_payload(vehicle_id, profile_id),
            headers=auth_headers(user),
        )
    )
    assert status == 201
    return body


def test_start_trip_unauthorized_returns_401(db_session_factory):
    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/start",
            trip_start_payload(
                "00000000-0000-0000-0000-000000000000",
                "00000000-0000-0000-0000-000000000000",
            ),
        )
    )

    assert status == 401
    assert body["detail"] == "Not authenticated."


def test_preview_route_valid_request_returns_route(google_route_sample):
    status, body = asyncio.run(
        request_app(
            "POST",
            "/routes/preview",
            {
                "origin": {
                    "label": "Current location",
                    "lat": 44.4268,
                    "lng": 26.1025,
                },
                "destination": {
                    "label": "Brasov",
                    "lat": 45.6579,
                    "lng": 25.6012,
                },
                "current_range": "120 km",
                "route_preferences": "chargers and restaurants",
            },
        )
    )

    assert status == 200
    assert body == google_route_sample


def test_preview_route_invalid_coords_returns_422():
    status, body = asyncio.run(
        request_app(
            "POST",
            "/routes/preview",
            {
                "origin": {
                    "label": "Current location",
                    "lat": 144,
                    "lng": 26.1025,
                },
                "destination": {
                    "label": "Brasov",
                    "lat": 45.6579,
                    "lng": 25.6012,
                },
            },
        )
    )

    assert status == 422
    assert "detail" in body


def test_start_trip_success_creates_trip_and_google_route(
    db_session_factory,
    google_route_sample,
):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)

    body = start_trip(db_session_factory, user, vehicle_id, profile_id)

    assert body["trip"]["status"] == "active"
    assert body["route"] == google_route_sample

    db = db_session_factory()
    try:
        assert db.query(Trip).count() == 1
        assert db.query(RouteOption).count() == 1
        route = db.query(RouteOption).one()
        assert route.provider == "google"
        assert route.is_selected is True
        assert float(route.distance_km) == google_route_sample["distance_km"]
        assert int(route.duration_minutes) == google_route_sample["duration_minutes"]
        assert route.polyline == google_route_sample["polyline"]
        assert db.query(Recommendation).count() == 0
        assert db.query(RecommendationItem).count() == 0
    finally:
        db.close()


def test_start_trip_vehicle_owned_by_another_user_returns_404(db_session_factory):
    owner = create_user(db_session_factory, email="owner@test.com")
    other_user = create_user(db_session_factory, email="other@test.com")
    vehicle_id, _ = create_setup(db_session_factory, owner)
    _, other_profile_id = create_setup(db_session_factory, other_user)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/start",
            trip_start_payload(vehicle_id, other_profile_id),
            headers=auth_headers(other_user),
        )
    )

    assert status == 404
    assert body["detail"] == "Vehicle not found."


def test_start_trip_driver_profile_owned_by_another_user_returns_404(
    db_session_factory,
):
    owner = create_user(db_session_factory, email="owner@test.com")
    other_user = create_user(db_session_factory, email="other@test.com")
    owner_vehicle_id, _ = create_setup(db_session_factory, owner)
    _, other_profile_id = create_setup(db_session_factory, other_user)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/start",
            trip_start_payload(owner_vehicle_id, other_profile_id),
            headers=auth_headers(owner),
        )
    )

    assert status == 404
    assert body["detail"] == "Driver profile not found."


def test_start_trip_routes_api_failure_returns_502(db_session_factory, monkeypatch):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)

    def fail_route(**_):
        from fastapi import HTTPException

        raise HTTPException(status_code=502, detail="Google Routes API returned an error.")

    monkeypatch.setattr("app.services.trip_service.get_route", fail_route)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/start",
            trip_start_payload(vehicle_id, profile_id),
            headers=auth_headers(user),
        )
    )

    assert status == 502
    assert body["detail"] == "Google Routes API returned an error."


def test_get_trip_unauthorized_returns_401(db_session_factory):
    status, body = asyncio.run(
        request_app("GET", "/trips/00000000-0000-0000-0000-000000000000")
    )

    assert status == 401
    assert body["detail"] == "Not authenticated."


def test_user_can_get_own_trip(db_session_factory):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)
    started = start_trip(db_session_factory, user, vehicle_id, profile_id)

    status, body = asyncio.run(
        request_app(
            "GET",
            f"/trips/{started['trip']['id']}",
            headers=auth_headers(user),
        )
    )

    assert status == 200
    assert body["trip"]["id"] == started["trip"]["id"]
    assert body["route"]["polyline"] == "encoded_polyline"


def test_user_cannot_get_another_users_trip(db_session_factory):
    owner = create_user(db_session_factory, email="owner@test.com")
    other_user = create_user(db_session_factory, email="other@test.com")
    vehicle_id, profile_id = create_setup(db_session_factory, owner)
    started = start_trip(db_session_factory, owner, vehicle_id, profile_id)

    status, body = asyncio.run(
        request_app(
            "GET",
            f"/trips/{started['trip']['id']}",
            headers=auth_headers(other_user),
        )
    )

    assert status == 404
    assert body["detail"] == "Trip not found."


def trip_update_payload(trip_id: str, fuel_level_percent: float = 18) -> dict:
    return {
        "trip_id": trip_id,
        "current_location": {
            "lat": 44.8,
            "lng": 26.05,
        },
        "vehicle_state": {
            "fuel_level_percent": fuel_level_percent,
            "battery_soc_percent": None,
            "estimated_range_km": 90,
            "odometer_km": 84250,
            "tire_pressure_status": "ok",
        },
    }


def test_update_trip_unauthorized_returns_401(db_session_factory):
    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/update",
            trip_update_payload("00000000-0000-0000-0000-000000000000"),
        )
    )

    assert status == 401
    assert body["detail"] == "Not authenticated."


def test_update_trip_success_creates_new_vehicle_state_snapshot(db_session_factory):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)
    started = start_trip(db_session_factory, user, vehicle_id, profile_id)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/update",
            trip_update_payload(started["trip"]["id"], fuel_level_percent=18),
            headers=auth_headers(user),
        )
    )

    assert status == 200
    assert body["status"] == "active"
    assert body["should_refresh_recommendation"] is True

    db = db_session_factory()
    try:
        assert db.query(VehicleStateSnapshot).count() == 2
        assert db.query(Recommendation).count() == 0
    finally:
        db.close()


def test_update_unknown_trip_returns_404(db_session_factory):
    user = create_user(db_session_factory)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/update",
            trip_update_payload("00000000-0000-0000-0000-000000000000"),
            headers=auth_headers(user),
        )
    )

    assert status == 404
    assert body["detail"] == "Trip not found."


def test_update_inactive_trip_returns_400(db_session_factory):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)
    started = start_trip(db_session_factory, user, vehicle_id, profile_id)

    db = db_session_factory()
    try:
        trip = db.query(Trip).filter(Trip.id == UUID(started["trip"]["id"])).first()
        trip.status = "completed"
        db.commit()
    finally:
        db.close()

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/update",
            trip_update_payload(started["trip"]["id"]),
            headers=auth_headers(user),
        )
    )

    assert status == 400
    assert body["detail"] == "Trip is not active."


def test_update_trip_low_fuel_returns_refresh_true(db_session_factory):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)
    started = start_trip(db_session_factory, user, vehicle_id, profile_id)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/update",
            trip_update_payload(started["trip"]["id"], fuel_level_percent=20),
            headers=auth_headers(user),
        )
    )

    assert status == 200
    assert body["should_refresh_recommendation"] is True


def test_update_trip_normal_state_returns_refresh_false(db_session_factory):
    user = create_user(db_session_factory)
    vehicle_id, profile_id = create_setup(db_session_factory, user)
    started = start_trip(db_session_factory, user, vehicle_id, profile_id)

    status, body = asyncio.run(
        request_app(
            "POST",
            "/trip/update",
            trip_update_payload(started["trip"]["id"], fuel_level_percent=60),
            headers=auth_headers(user),
        )
    )

    assert status == 200
    assert body["should_refresh_recommendation"] is False

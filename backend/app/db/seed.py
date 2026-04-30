from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any

from app.db.database import SessionLocal
from app.db import models as dbm


DEMO_PASSWORD_HASH = "demo_password_hash"


# ---------------------------------------------------------------------------
# Generic helpers
# ---------------------------------------------------------------------------

def get_or_create_user(
    db,
    email: str,
    full_name: str,
    password_hash: str = DEMO_PASSWORD_HASH,
):
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
    profile = (
        db.query(dbm.DriverProfile)
        .filter(
            dbm.DriverProfile.user_id == user_id,
            dbm.DriverProfile.profile_name == profile_name,
        )
        .first()
    )

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


def get_or_create_vehicle(
    db,
    user_id,
    model: str,
    year: int,
    powertrain: str,
    vin: str,
    connector_type: str | None = None,
    battery_capacity_kwh: float | None = None,
    fuel_tank_liters: float | None = None,
    consumption_kwh_per_100km: float | None = None,
    consumption_l_per_100km: float | None = None,
    service_interval_km: int | None = None,
    service_interval_months: int | None = None,
):
    vehicle = db.query(dbm.Vehicle).filter(dbm.Vehicle.vin == vin).first()

    if vehicle:
        print(f"Vehicle already exists: {model} / {vin}")
        return vehicle

    vehicle = dbm.Vehicle(
        user_id=user_id,
        vin=vin,
        model=model,
        year=year,
        powertrain=powertrain,
        connector_type=connector_type,
        battery_capacity_kwh=battery_capacity_kwh,
        fuel_tank_liters=fuel_tank_liters,
        consumption_kwh_per_100km=consumption_kwh_per_100km,
        consumption_l_per_100km=consumption_l_per_100km,
        service_interval_km=service_interval_km,
        service_interval_months=service_interval_months,
    )

    db.add(vehicle)
    db.flush()

    print(f"Created vehicle: {model}")
    return vehicle


def get_or_create_vehicle_state(
    db,
    vehicle_id,
    battery_soc_percent: float | None,
    fuel_level_percent: float | None,
    estimated_range_km: float,
    odometer_km: float,
    tire_pressure_status: str = "ok",
):
    existing = (
        db.query(dbm.VehicleStateSnapshot)
        .filter(dbm.VehicleStateSnapshot.vehicle_id == vehicle_id)
        .first()
    )

    if existing:
        print(f"Vehicle state already exists for vehicle: {vehicle_id}")
        return existing

    state = dbm.VehicleStateSnapshot(
        vehicle_id=vehicle_id,
        battery_soc_percent=battery_soc_percent,
        fuel_level_percent=fuel_level_percent,
        estimated_range_km=estimated_range_km,
        odometer_km=odometer_km,
        tire_pressure_status=tire_pressure_status,
    )

    db.add(state)
    db.flush()

    print(f"Created vehicle state for vehicle: {vehicle_id}")
    return state


def get_or_create_location(
    db,
    key: str,
    name: str,
    location_type: str,
    address: str,
    city: str,
    country: str,
    latitude: float,
    longitude: float,
    opening_hours: dict[str, Any] | None,
    rating: float | None,
    amenities: list[str] | dict[str, Any] | None,
):
    location = (
        db.query(dbm.Location)
        .filter(
            dbm.Location.name == name,
            dbm.Location.city == city,
            dbm.Location.location_type == location_type,
        )
        .first()
    )

    if location:
        print(f"Location already exists: {name}")
        return location

    location = dbm.Location(
        name=name,
        location_type=location_type,
        address=address,
        city=city,
        country=country,
        latitude=latitude,
        longitude=longitude,
        opening_hours=opening_hours,
        rating=rating,
        amenities=amenities,
    )

    db.add(location)
    db.flush()

    print(f"Created location: {name}")
    return location


def get_or_create_charging_station(
    db,
    location_id,
    connector_types: list[str],
    max_power_kw: float,
    price_per_kwh: float,
    availability_status: str,
    reliability_score: float,
):
    station = (
        db.query(dbm.ChargingStation)
        .filter(dbm.ChargingStation.location_id == location_id)
        .first()
    )

    if station:
        print(f"Charging station already exists for location: {location_id}")
        return station

    station = dbm.ChargingStation(
        location_id=location_id,
        connector_types=connector_types,
        max_power_kw=max_power_kw,
        price_per_kwh=price_per_kwh,
        availability_status=availability_status,
        reliability_score=reliability_score,
    )

    db.add(station)
    db.flush()

    print(f"Created charging station for location: {location_id}")
    return station


def get_or_create_fuel_station(
    db,
    location_id,
    fuel_types: list[str],
    price_per_liter: float,
    availability_status: str,
):
    station = (
        db.query(dbm.FuelStation)
        .filter(dbm.FuelStation.location_id == location_id)
        .first()
    )

    if station:
        print(f"Fuel station already exists for location: {location_id}")
        return station

    station = dbm.FuelStation(
        location_id=location_id,
        fuel_types=fuel_types,
        price_per_liter=price_per_liter,
        availability_status=availability_status,
    )

    db.add(station)
    db.flush()

    print(f"Created fuel station for location: {location_id}")
    return station


def get_or_create_service_center(
    db,
    location_id,
    supported_brands: list[str],
    service_types: list[str],
    is_authorized: bool,
    booking_url: str | None,
    availability_status: str,
):
    center = (
        db.query(dbm.ServiceCenter)
        .filter(dbm.ServiceCenter.location_id == location_id)
        .first()
    )

    if center:
        print(f"Service center already exists for location: {location_id}")
        return center

    center = dbm.ServiceCenter(
        location_id=location_id,
        supported_brands=supported_brands,
        service_types=service_types,
        is_authorized=is_authorized,
        booking_url=booking_url,
        availability_status=availability_status,
    )

    db.add(center)
    db.flush()

    print(f"Created service center for location: {location_id}")
    return center


def get_or_create_partner(
    db,
    name: str,
    partner_type: str,
    priority_level: int,
    commission_model: str,
    active: bool = True,
):
    partner = db.query(dbm.Partner).filter(dbm.Partner.name == name).first()

    if partner:
        print(f"Partner already exists: {name}")
        return partner

    partner = dbm.Partner(
        name=name,
        partner_type=partner_type,
        priority_level=priority_level,
        commission_model=commission_model,
        active=active,
    )

    db.add(partner)
    db.flush()

    print(f"Created partner: {name}")
    return partner


def get_or_create_partner_location(db, partner_id, location_id):
    partner_location = (
        db.query(dbm.PartnerLocation)
        .filter(
            dbm.PartnerLocation.partner_id == partner_id,
            dbm.PartnerLocation.location_id == location_id,
        )
        .first()
    )

    if partner_location:
        print(f"Partner location already exists: {partner_id} -> {location_id}")
        return partner_location

    partner_location = dbm.PartnerLocation(
        partner_id=partner_id,
        location_id=location_id,
        active=True,
    )

    db.add(partner_location)
    db.flush()

    print(f"Created partner location: {partner_id} -> {location_id}")
    return partner_location


def get_or_create_partner_offer(
    db,
    partner_id,
    partner_location_id,
    title: str,
    description: str,
    offer_type: str,
    discount_percent: float | None,
    fixed_discount_amount: float | None,
    loyalty_points: int | None,
    commission_value: float | None,
    campaign_priority: int,
    valid_from: datetime,
    valid_until: datetime,
    active: bool = True,
):
    offer = (
        db.query(dbm.PartnerOffer)
        .filter(
            dbm.PartnerOffer.partner_id == partner_id,
            dbm.PartnerOffer.title == title,
        )
        .first()
    )

    if offer:
        print(f"Partner offer already exists: {title}")
        return offer

    offer = dbm.PartnerOffer(
        partner_id=partner_id,
        partner_location_id=partner_location_id,
        title=title,
        description=description,
        offer_type=offer_type,
        discount_percent=discount_percent,
        fixed_discount_amount=fixed_discount_amount,
        loyalty_points=loyalty_points,
        commission_value=commission_value,
        campaign_priority=campaign_priority,
        valid_from=valid_from,
        valid_until=valid_until,
        active=active,
    )

    db.add(offer)
    db.flush()

    print(f"Created partner offer: {title}")
    return offer


def get_or_create_loyalty_account(db, user_id, points_balance: int, tier: str):
    account = (
        db.query(dbm.LoyaltyAccount)
        .filter(dbm.LoyaltyAccount.user_id == user_id)
        .first()
    )

    if account:
        print(f"Loyalty account already exists for user: {user_id}")
        return account

    account = dbm.LoyaltyAccount(
        user_id=user_id,
        points_balance=points_balance,
        tier=tier,
    )

    db.add(account)
    db.flush()

    print(f"Created loyalty account for user: {user_id}")
    return account


# ---------------------------------------------------------------------------
# Seed data
# ---------------------------------------------------------------------------

def seed() -> None:
    db = SessionLocal()

    try:
        now = datetime.now(timezone.utc)
        valid_from = now - timedelta(days=7)
        valid_until = now + timedelta(days=90)

        # -------------------------------------------------------------------
        # 5 users
        # -------------------------------------------------------------------

        users = {
            "balanced": get_or_create_user(
                db,
                email="demo@davaroutes.com",
                full_name="Demo Balanced Driver",
            ),
            "family_ev": get_or_create_user(
                db,
                email="ev.family@davaroutes.com",
                full_name="Elena Popescu",
            ),
            "business": get_or_create_user(
                db,
                email="business.driver@davaroutes.com",
                full_name="Andrei Ionescu",
            ),
            "budget_ev": get_or_create_user(
                db,
                email="budget.ev@davaroutes.com",
                full_name="Maria Stan",
            ),
            "service_due": get_or_create_user(
                db,
                email="service.due@davaroutes.com",
                full_name="Vlad Marinescu",
            ),
        }

        # -------------------------------------------------------------------
        # 5 driver profiles
        # -------------------------------------------------------------------

        profiles = {
            "balanced": get_or_create_driver_profile(
                db,
                users["balanced"].id,
                profile_name="Balanced Daily Profile",
                profile_type="balanced",
                preferred_amenities=["restaurant", "restroom", "parking"],
                preferred_brands=["OMV", "MOL", "Kaufland"],
                avoid_tolls=False,
                avoid_highways=False,
                max_detour_minutes=10,
                break_frequency_minutes=120,
            ),
            "family_ev": get_or_create_driver_profile(
                db,
                users["family_ev"].id,
                profile_name="Family EV Profile",
                profile_type="family",
                preferred_amenities=["restaurant", "restroom", "playground", "safe_parking"],
                preferred_brands=["Kaufland", "McDonald's", "Ionity", "Starbucks"],
                avoid_tolls=False,
                avoid_highways=False,
                max_detour_minutes=15,
                break_frequency_minutes=90,
            ),
            "business": get_or_create_driver_profile(
                db,
                users["business"].id,
                profile_name="Business Fast Profile",
                profile_type="business",
                preferred_amenities=["coffee", "wifi", "fast_service", "parking"],
                preferred_brands=["OMV", "Starbucks", "MOL"],
                avoid_tolls=False,
                avoid_highways=False,
                max_detour_minutes=5,
                break_frequency_minutes=180,
            ),
            "budget_ev": get_or_create_driver_profile(
                db,
                users["budget_ev"].id,
                profile_name="Budget EV Profile",
                profile_type="cheapest",
                preferred_amenities=["restroom", "parking"],
                preferred_brands=["Kaufland", "MOL", "EcoCharge"],
                avoid_tolls=True,
                avoid_highways=False,
                max_detour_minutes=12,
                break_frequency_minutes=150,
            ),
            "service_due": get_or_create_driver_profile(
                db,
                users["service_due"].id,
                profile_name="Service Aware Profile",
                profile_type="balanced",
                preferred_amenities=["restaurant", "restroom", "service"],
                preferred_brands=["Dacia Service Network", "Toyota Service Network", "OMV"],
                avoid_tolls=False,
                avoid_highways=False,
                max_detour_minutes=12,
                break_frequency_minutes=120,
            ),
        }

        # -------------------------------------------------------------------
        # 5 vehicles
        # -------------------------------------------------------------------

        vehicles = {
            "balanced": get_or_create_vehicle(
                db,
                user_id=users["balanced"].id,
                vin="DRV-ICE-LOGAN-001",
                model="Dacia Logan",
                year=2024,
                powertrain="ICE",
                fuel_tank_liters=50,
                consumption_l_per_100km=7.2,
                service_interval_km=15000,
                service_interval_months=12,
            ),
            "family_ev": get_or_create_vehicle(
                db,
                user_id=users["family_ev"].id,
                vin="DRV-EV-MODEL3-002",
                model="Tesla Model 3",
                year=2024,
                powertrain="EV",
                connector_type="CCS2",
                battery_capacity_kwh=75,
                consumption_kwh_per_100km=16.5,
                service_interval_km=20000,
                service_interval_months=12,
            ),
            "business": get_or_create_vehicle(
                db,
                user_id=users["business"].id,
                vin="DRV-ICE-BMW320-003",
                model="BMW 320d",
                year=2023,
                powertrain="ICE",
                fuel_tank_liters=59,
                consumption_l_per_100km=5.4,
                service_interval_km=20000,
                service_interval_months=12,
            ),
            "budget_ev": get_or_create_vehicle(
                db,
                user_id=users["budget_ev"].id,
                vin="DRV-EV-KONA-004",
                model="Hyundai Kona Electric",
                year=2022,
                powertrain="EV",
                connector_type="CCS2",
                battery_capacity_kwh=64,
                consumption_kwh_per_100km=15.9,
                service_interval_km=15000,
                service_interval_months=12,
            ),
            "service_due": get_or_create_vehicle(
                db,
                user_id=users["service_due"].id,
                vin="DRV-HYB-COROLLA-005",
                model="Toyota Corolla Hybrid",
                year=2021,
                powertrain="HYBRID",
                fuel_tank_liters=43,
                consumption_l_per_100km=4.7,
                service_interval_km=15000,
                service_interval_months=12,
            ),
        }

        # -------------------------------------------------------------------
        # 5 vehicle state snapshots
        # -------------------------------------------------------------------

        get_or_create_vehicle_state(
            db,
            vehicles["balanced"].id,
            battery_soc_percent=None,
            fuel_level_percent=48,
            estimated_range_km=330,
            odometer_km=42000,
        )
        get_or_create_vehicle_state(
            db,
            vehicles["family_ev"].id,
            battery_soc_percent=22,
            fuel_level_percent=None,
            estimated_range_km=145,
            odometer_km=28100,
        )
        get_or_create_vehicle_state(
            db,
            vehicles["business"].id,
            battery_soc_percent=None,
            fuel_level_percent=76,
            estimated_range_km=720,
            odometer_km=63800,
        )
        get_or_create_vehicle_state(
            db,
            vehicles["budget_ev"].id,
            battery_soc_percent=38,
            fuel_level_percent=None,
            estimated_range_km=210,
            odometer_km=51700,
        )
        get_or_create_vehicle_state(
            db,
            vehicles["service_due"].id,
            battery_soc_percent=None,
            fuel_level_percent=61,
            estimated_range_km=520,
            odometer_km=14860,
        )

        # -------------------------------------------------------------------
        # 35 locations
        # -------------------------------------------------------------------

        opening_24_7 = {"mon_sun": "00:00-24:00"}
        opening_daily = {"mon_sun": "07:00-23:00"}
        opening_business = {"mon_fri": "08:00-18:00", "sat": "09:00-14:00", "sun": "closed"}

        locations = {}

        # 8 charging locations
        charging_locations = [
            ("ionity_ploiesti", "Ionity Ploiesti FastCharge", "DN1, Ploiesti", "Ploiesti", 44.9362, 26.0123, 4.6, ["restroom", "parking", "fast_charging"]),
            ("kaufland_ploiesti_charge", "Kaufland Charge Ploiesti", "Strada Vestului 12", "Ploiesti", 44.9404, 26.0255, 4.4, ["restaurant", "restroom", "shopping", "safe_parking"]),
            ("ecocharge_campina", "EcoCharge Campina", "Calea Doftanei 4", "Campina", 45.1269, 25.7387, 4.0, ["restroom", "parking"]),
            ("mol_plugee_sinaia", "MOL Plugee Sinaia", "DN1 Sinaia", "Sinaia", 45.3514, 25.5489, 4.3, ["coffee", "restroom", "parking"]),
            ("mountaincharge_predeal", "MountainCharge Predeal", "Strada Garii 8", "Predeal", 45.5008, 25.5713, 4.2, ["scenic_view", "coffee", "restroom"]),
            ("citycharge_brasov", "CityCharge Brasov", "Calea Bucuresti 99", "Brasov", 45.6427, 25.5887, 4.1, ["parking", "shopping"]),
            ("hotel_ev_brasov", "Hotel EV Charger Brasov", "Strada Lunga 22", "Brasov", 45.6510, 25.5920, 4.5, ["hotel", "restaurant", "parking"]),
            ("slowcharge_busteni", "SlowCharge Busteni", "Bulevardul Libertatii 101", "Busteni", 45.4104, 25.5345, 3.8, ["restroom", "scenic_view"]),
        ]

        for key, name, address, city, lat, lng, rating, amenities in charging_locations:
            locations[key] = get_or_create_location(
                db,
                key=key,
                name=name,
                location_type="charging",
                address=address,
                city=city,
                country="Romania",
                latitude=lat,
                longitude=lng,
                opening_hours=opening_24_7,
                rating=rating,
                amenities=amenities,
            )

        # 7 fuel locations
        fuel_locations = [
            ("omv_ploiesti", "OMV Ploiesti Nord", "DN1 Km 58", "Ploiesti", 44.9450, 26.0180, 4.5, ["restaurant", "restroom", "coffee", "parking"]),
            ("mol_campina", "MOL Campina", "Bulevardul Carol I 88", "Campina", 45.1305, 25.7360, 4.3, ["restroom", "coffee", "parking"]),
            ("petrom_sinaia", "Petrom Sinaia", "DN1 Sinaia Sud", "Sinaia", 45.3321, 25.5631, 4.1, ["restroom", "parking"]),
            ("rompetrol_busteni", "Rompetrol Busteni", "DN1 Busteni", "Busteni", 45.4070, 25.5368, 4.2, ["restaurant", "restroom", "parking"]),
            ("lukoil_brasov", "Lukoil Brasov", "Calea Bucuresti 120", "Brasov", 45.6332, 25.6104, 3.9, ["restroom", "parking"]),
            ("omv_predeal", "OMV Predeal", "DN1 Predeal", "Predeal", 45.5060, 25.5752, 4.4, ["coffee", "restroom", "parking"]),
            ("mol_brasov_centru", "MOL Brasov Centru", "Strada Harmanului 45", "Brasov", 45.6573, 25.6255, 4.2, ["coffee", "restroom", "car_wash"]),
        ]

        for key, name, address, city, lat, lng, rating, amenities in fuel_locations:
            locations[key] = get_or_create_location(
                db,
                key=key,
                name=name,
                location_type="fuel",
                address=address,
                city=city,
                country="Romania",
                latitude=lat,
                longitude=lng,
                opening_hours=opening_24_7,
                rating=rating,
                amenities=amenities,
            )

        # 8 restaurants / cafés
        restaurant_locations = [
            ("mcd_ploiesti", "McDonald's Ploiesti DN1", "DN1 Shopping Area", "Ploiesti", 44.9484, 26.0221, 4.2, ["restaurant", "restroom", "playground", "family_friendly", "parking"]),
            ("starbucks_ploiesti", "Starbucks Ploiesti", "AFI Ploiesti", "Ploiesti", 44.9367, 26.0310, 4.4, ["coffee", "wifi", "restroom", "parking"]),
            ("family_grill_campina", "Family Grill Campina", "Strada Republicii 31", "Campina", 45.1281, 25.7379, 4.5, ["restaurant", "restroom", "playground", "family_friendly"]),
            ("mountain_view_cafe_sinaia", "Mountain View Cafe Sinaia", "Aleea Pelesului 2", "Sinaia", 45.3591, 25.5428, 4.7, ["coffee", "scenic_view", "restroom"]),
            ("quick_lunch_brasov", "Quick Lunch Brasov", "Calea Bucuresti 80", "Brasov", 45.6366, 25.6042, 4.0, ["restaurant", "fast_service", "parking"]),
            ("burger_busteni", "Burger House Busteni", "Strada Valea Alba 17", "Busteni", 45.4142, 25.5359, 4.1, ["restaurant", "restroom", "family_friendly"]),
            ("predeal_cafe", "Predeal Coffee Stop", "Strada Garii 5", "Predeal", 45.5051, 25.5733, 4.3, ["coffee", "wifi", "restroom"]),
            ("brasov_family_bistro", "Brasov Family Bistro", "Strada Republicii 14", "Brasov", 45.6421, 25.5908, 4.6, ["restaurant", "restroom", "family_friendly"]),
        ]

        for key, name, address, city, lat, lng, rating, amenities in restaurant_locations:
            locations[key] = get_or_create_location(
                db,
                key=key,
                name=name,
                location_type="restaurant",
                address=address,
                city=city,
                country="Romania",
                latitude=lat,
                longitude=lng,
                opening_hours=opening_daily,
                rating=rating,
                amenities=amenities,
            )

        # 3 hotels
        hotel_locations = [
            ("hotel_brasov_central", "Hotel Brasov Central", "Strada Muresenilor 10", "Brasov", 45.6439, 25.5893, 4.5, ["hotel", "parking", "restaurant", "ev_charging"]),
            ("hotel_sinaia_alpin", "Hotel Alpin Sinaia", "Strada Furnica 12", "Sinaia", 45.3522, 25.5510, 4.4, ["hotel", "parking", "restaurant", "scenic_view"]),
            ("hotel_predeal_comfort", "Hotel Comfort Predeal", "Bulevardul Libertatii 33", "Predeal", 45.5080, 25.5747, 4.1, ["hotel", "parking", "breakfast"]),
        ]

        for key, name, address, city, lat, lng, rating, amenities in hotel_locations:
            locations[key] = get_or_create_location(
                db,
                key=key,
                name=name,
                location_type="hotel",
                address=address,
                city=city,
                country="Romania",
                latitude=lat,
                longitude=lng,
                opening_hours=opening_24_7,
                rating=rating,
                amenities=amenities,
            )

        # 5 service centers
        service_locations = [
            ("dacia_service_ploiesti", "Dacia Authorized Service Ploiesti", "Strada Service Auto 1", "Ploiesti", 44.9440, 26.0401, 4.5, ["service", "authorized", "waiting_area"]),
            ("tesla_service_brasov", "Tesla Service Brasov", "Calea Bucuresti 150", "Brasov", 45.6283, 25.6148, 4.6, ["service", "authorized", "ev_service"]),
            ("bosch_service_campina", "Bosch Car Service Campina", "Strada Atelierelor 9", "Campina", 45.1228, 25.7421, 4.2, ["service", "multi_brand"]),
            ("toyota_service_brasov", "Toyota Authorized Service Brasov", "Strada Harmanului 200", "Brasov", 45.6630, 25.6340, 4.7, ["service", "authorized", "hybrid_service"]),
            ("generic_auto_sinaia", "Generic Auto Service Sinaia", "DN1 Service Area", "Sinaia", 45.3450, 25.5589, 3.9, ["service", "multi_brand"]),
        ]

        for key, name, address, city, lat, lng, rating, amenities in service_locations:
            locations[key] = get_or_create_location(
                db,
                key=key,
                name=name,
                location_type="service",
                address=address,
                city=city,
                country="Romania",
                latitude=lat,
                longitude=lng,
                opening_hours=opening_business,
                rating=rating,
                amenities=amenities,
            )

        # 4 rest / parking locations to reach 35 total
        other_locations = [
            ("rest_area_comarnic", "Comarnic Rest Area", "DN1 Comarnic", "Comarnic", 45.2471, 25.6330, 4.0, "rest", ["restroom", "parking", "scenic_view"]),
            ("parking_peles", "Peles Castle Parking", "Aleea Pelesului", "Sinaia", 45.3599, 25.5420, 4.5, "parking", ["parking", "scenic_view"]),
            ("rest_area_predeal", "Predeal Rest Area", "DN1 Predeal", "Predeal", 45.5022, 25.5700, 4.1, "rest", ["restroom", "parking"]),
            ("parking_brasov_old_town", "Brasov Old Town Parking", "Strada George Baritiu", "Brasov", 45.6417, 25.5866, 4.2, "parking", ["parking", "city_center"]),
        ]

        for key, name, address, city, lat, lng, rating, location_type, amenities in other_locations:
            locations[key] = get_or_create_location(
                db,
                key=key,
                name=name,
                location_type=location_type,
                address=address,
                city=city,
                country="Romania",
                latitude=lat,
                longitude=lng,
                opening_hours=opening_24_7,
                rating=rating,
                amenities=amenities,
            )

        # -------------------------------------------------------------------
        # 8 charging stations
        # -------------------------------------------------------------------

        get_or_create_charging_station(db, locations["ionity_ploiesti"].id, ["CCS2"], 350, 3.20, "available", 0.96)
        get_or_create_charging_station(db, locations["kaufland_ploiesti_charge"].id, ["CCS2", "Type2"], 150, 2.30, "available", 0.90)
        get_or_create_charging_station(db, locations["ecocharge_campina"].id, ["CCS2", "Type2"], 50, 1.80, "available", 0.82)
        get_or_create_charging_station(db, locations["mol_plugee_sinaia"].id, ["CCS2"], 150, 2.55, "busy", 0.88)
        get_or_create_charging_station(db, locations["mountaincharge_predeal"].id, ["CCS2"], 100, 2.75, "available", 0.84)
        get_or_create_charging_station(db, locations["citycharge_brasov"].id, ["CCS2", "Type2"], 75, 2.10, "available", 0.86)
        get_or_create_charging_station(db, locations["hotel_ev_brasov"].id, ["Type2", "CCS2"], 50, 2.00, "available", 0.81)
        get_or_create_charging_station(db, locations["slowcharge_busteni"].id, ["Type2"], 22, 1.60, "available", 0.76)

        # -------------------------------------------------------------------
        # 7 fuel stations
        # -------------------------------------------------------------------

        get_or_create_fuel_station(db, locations["omv_ploiesti"].id, ["gasoline", "diesel", "lpg"], 7.35, "available")
        get_or_create_fuel_station(db, locations["mol_campina"].id, ["gasoline", "diesel"], 7.29, "available")
        get_or_create_fuel_station(db, locations["petrom_sinaia"].id, ["gasoline", "diesel"], 7.22, "available")
        get_or_create_fuel_station(db, locations["rompetrol_busteni"].id, ["gasoline", "diesel"], 7.41, "busy")
        get_or_create_fuel_station(db, locations["lukoil_brasov"].id, ["gasoline", "diesel", "lpg"], 7.15, "available")
        get_or_create_fuel_station(db, locations["omv_predeal"].id, ["gasoline", "diesel"], 7.38, "available")
        get_or_create_fuel_station(db, locations["mol_brasov_centru"].id, ["gasoline", "diesel"], 7.28, "available")

        # -------------------------------------------------------------------
        # 5 service centers
        # -------------------------------------------------------------------

        get_or_create_service_center(
            db,
            locations["dacia_service_ploiesti"].id,
            ["Dacia", "Renault"],
            ["inspection", "oil_change", "brakes", "diagnostics"],
            True,
            "https://service.example.com/dacia-ploiesti",
            "available",
        )
        get_or_create_service_center(
            db,
            locations["tesla_service_brasov"].id,
            ["Tesla"],
            ["ev_diagnostics", "battery_check", "software_update"],
            True,
            "https://service.example.com/tesla-brasov",
            "available",
        )
        get_or_create_service_center(
            db,
            locations["bosch_service_campina"].id,
            ["Dacia", "BMW", "Toyota", "Hyundai", "Tesla"],
            ["inspection", "diagnostics", "tires", "brakes"],
            False,
            "https://service.example.com/bosch-campina",
            "available",
        )
        get_or_create_service_center(
            db,
            locations["toyota_service_brasov"].id,
            ["Toyota"],
            ["hybrid_check", "inspection", "oil_change", "diagnostics"],
            True,
            "https://service.example.com/toyota-brasov",
            "available",
        )
        get_or_create_service_center(
            db,
            locations["generic_auto_sinaia"].id,
            ["Dacia", "BMW", "Toyota", "Hyundai"],
            ["inspection", "tires", "brakes"],
            False,
            "https://service.example.com/generic-sinaia",
            "busy",
        )

        # -------------------------------------------------------------------
        # 8 partners
        # -------------------------------------------------------------------

        partners = {
            "ionity": get_or_create_partner(db, "Ionity", "charging_network", 8, "revenue_share"),
            "omv": get_or_create_partner(db, "OMV", "fuel_chain", 7, "cpa"),
            "mol": get_or_create_partner(db, "MOL", "fuel_chain", 6, "cpa"),
            "mcdonalds": get_or_create_partner(db, "McDonald's", "restaurant", 7, "cpc"),
            "starbucks": get_or_create_partner(db, "Starbucks", "restaurant", 6, "cpc"),
            "kaufland": get_or_create_partner(db, "Kaufland", "charging_network", 5, "fixed"),
            "dacia_service": get_or_create_partner(db, "Dacia Service Network", "service", 9, "cpa"),
            "toyota_service": get_or_create_partner(db, "Toyota Service Network", "service", 8, "cpa"),
        }

        # -------------------------------------------------------------------
        # Partner locations
        # -------------------------------------------------------------------

        partner_locations = {
            "ionity_ploiesti": get_or_create_partner_location(db, partners["ionity"].id, locations["ionity_ploiesti"].id),
            "omv_ploiesti": get_or_create_partner_location(db, partners["omv"].id, locations["omv_ploiesti"].id),
            "omv_predeal": get_or_create_partner_location(db, partners["omv"].id, locations["omv_predeal"].id),
            "mol_campina": get_or_create_partner_location(db, partners["mol"].id, locations["mol_campina"].id),
            "mol_brasov": get_or_create_partner_location(db, partners["mol"].id, locations["mol_brasov_centru"].id),
            "mcd_ploiesti": get_or_create_partner_location(db, partners["mcdonalds"].id, locations["mcd_ploiesti"].id),
            "starbucks_ploiesti": get_or_create_partner_location(db, partners["starbucks"].id, locations["starbucks_ploiesti"].id),
            "kaufland_ploiesti": get_or_create_partner_location(db, partners["kaufland"].id, locations["kaufland_ploiesti_charge"].id),
            "dacia_service": get_or_create_partner_location(db, partners["dacia_service"].id, locations["dacia_service_ploiesti"].id),
            "toyota_service": get_or_create_partner_location(db, partners["toyota_service"].id, locations["toyota_service_brasov"].id),
            "hotel_brasov": get_or_create_partner_location(db, partners["kaufland"].id, locations["hotel_ev_brasov"].id),
            "mol_sinaia": get_or_create_partner_location(db, partners["mol"].id, locations["mol_plugee_sinaia"].id),
        }

        # -------------------------------------------------------------------
        # 12 partner offers
        # -------------------------------------------------------------------

        get_or_create_partner_offer(
            db,
            partners["ionity"].id,
            partner_locations["ionity_ploiesti"].id,
            "10% off Ionity fast charging",
            "Save 10% on fast charging sessions at Ionity Ploiesti.",
            "discount",
            10,
            None,
            80,
            3.50,
            8,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["kaufland"].id,
            partner_locations["kaufland_ploiesti"].id,
            "Free parking while charging",
            "Charge at Kaufland and receive free parking during the session.",
            "perk",
            None,
            None,
            60,
            2.00,
            6,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["omv"].id,
            partner_locations["omv_ploiesti"].id,
            "50 loyalty points for OMV fuel stop",
            "Earn 50 points when selecting OMV Ploiesti as your fuel stop.",
            "loyalty_points",
            None,
            None,
            50,
            1.80,
            7,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["omv"].id,
            partner_locations["omv_predeal"].id,
            "Coffee bundle at OMV Predeal",
            "Fuel stop bundle with coffee discount at OMV Predeal.",
            "bundle",
            5,
            None,
            40,
            1.50,
            5,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["mol"].id,
            partner_locations["mol_campina"].id,
            "70 loyalty points at MOL Campina",
            "Earn 70 loyalty points for fuel or charging at MOL Campina.",
            "loyalty_points",
            None,
            None,
            70,
            1.90,
            6,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["mol"].id,
            partner_locations["mol_sinaia"].id,
            "MOL Plugee EV charging perk",
            "EV charging stop with coffee partner perk near Sinaia.",
            "perk",
            None,
            None,
            65,
            2.20,
            6,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["mcdonalds"].id,
            partner_locations["mcd_ploiesti"].id,
            "Family meal discount",
            "Family-friendly meal offer for route stops near Ploiesti.",
            "discount",
            12,
            None,
            35,
            1.20,
            7,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["starbucks"].id,
            partner_locations["starbucks_ploiesti"].id,
            "Business coffee stop",
            "Coffee discount for business drivers needing a quick break.",
            "discount",
            15,
            None,
            25,
            1.10,
            5,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["dacia_service"].id,
            partner_locations["dacia_service"].id,
            "Dacia inspection discount",
            "Discounted inspection at authorized Dacia Service Ploiesti.",
            "discount",
            10,
            None,
            120,
            5.00,
            9,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["toyota_service"].id,
            partner_locations["toyota_service"].id,
            "Toyota hybrid check bonus",
            "Earn 150 points for a Toyota hybrid service check.",
            "loyalty_points",
            None,
            None,
            150,
            5.50,
            9,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["kaufland"].id,
            partner_locations["hotel_brasov"].id,
            "Destination charging hotel perk",
            "Hotel guest perk for EV drivers charging overnight in Brasov.",
            "perk",
            None,
            None,
            90,
            2.50,
            4,
            valid_from,
            valid_until,
        )
        get_or_create_partner_offer(
            db,
            partners["mol"].id,
            partner_locations["mol_brasov"].id,
            "Brasov city fuel cashback",
            "Small cashback for selecting MOL Brasov as your destination fuel stop.",
            "cashback",
            None,
            8,
            45,
            1.70,
            5,
            valid_from,
            valid_until,
        )

        # -------------------------------------------------------------------
        # 5 loyalty accounts
        # -------------------------------------------------------------------

        get_or_create_loyalty_account(db, users["balanced"].id, 250, "bronze")
        get_or_create_loyalty_account(db, users["family_ev"].id, 780, "silver")
        get_or_create_loyalty_account(db, users["business"].id, 1150, "gold")
        get_or_create_loyalty_account(db, users["budget_ev"].id, 430, "bronze")
        get_or_create_loyalty_account(db, users["service_due"].id, 960, "silver")

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
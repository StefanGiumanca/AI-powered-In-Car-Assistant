import uuid
from enum import Enum

from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.db.database import Base


# ---------------------------------------------------------------------------
# ENUMS
# ---------------------------------------------------------------------------

class StopType(str, Enum):
    charging = "charging"
    fuel = "fuel"
    food = "food"
    hotel = "hotel"
    service = "service"
    rest = "rest"
    parking = "parking"


class UrgencyLevel(str, Enum):
    low = "low"
    medium = "medium"
    high = "high"
    overdue = "overdue"


class RecommendationType(str, Enum):
    route = "route"
    stop = "stop"
    service = "service"
    itinerary = "itinerary"


class RecommendationItemType(str, Enum):
    route = "route"
    charging_stop = "charging_stop"
    fuel_stop = "fuel_stop"
    food_stop = "food_stop"
    hotel_stop = "hotel_stop"
    service_center = "service_center"
    rest_stop = "rest_stop"
    parking = "parking"


class LoyaltyTransactionType(str, Enum):
    earned = "earned"
    redeemed = "redeemed"
    expired = "expired"
    adjusted = "adjusted"


class AssistantInputType(str, Enum):
    text = "text"
    voice = "voice"


class RecommendationEventType(str, Enum):
    generated = "generated"
    viewed = "viewed"
    accepted = "accepted"
    rejected = "rejected"
    offer_viewed = "offer_viewed"
    offer_accepted = "offer_accepted"
    loyalty_points_awarded = "loyalty_points_awarded"
    service_alert_triggered = "service_alert_triggered"
    assistant_query_received = "assistant_query_received"

class ProfileType(str, Enum):
    fastest = "fastest"
    cheapest = "cheapest"
    family = "family"
    scenic = "scenic"
    balanced = "balanced"
    business = "business"


class PowertrainType(str, Enum):
    EV = "EV"
    ICE = "ICE"
    HYBRID = "HYBRID"


class TripStatus(str, Enum):
    planned = "planned"
    active = "active"
    completed = "completed"
    cancelled = "cancelled"


class LocationType(str, Enum):
    charging = "charging"
    fuel = "fuel"
    restaurant = "restaurant"
    hotel = "hotel"
    service = "service"
    parking = "parking"
    rest = "rest"
    other = "other"


class AvailabilityStatus(str, Enum):
    available = "available"
    busy = "busy"
    unavailable = "unavailable"
    unknown = "unknown"


class PartnerType(str, Enum):
    charging_network = "charging_network"
    fuel_chain = "fuel_chain"
    restaurant = "restaurant"
    hotel = "hotel"
    service = "service"
    parking = "parking"
    roadside_assistance = "roadside_assistance"
    other = "other"


class CommissionModel(str, Enum):
    cpa = "cpa"
    cpc = "cpc"
    revenue_share = "revenue_share"
    fixed = "fixed"
    none = "none"


class OfferType(str, Enum):
    discount = "discount"
    perk = "perk"
    loyalty_points = "loyalty_points"
    bundle = "bundle"
    cashback = "cashback"


# ---------------------------------------------------------------------------
# 1. USERS
# ---------------------------------------------------------------------------

class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    email = Column(String(255), nullable=False, unique=True, index=True)
    password_hash = Column(String(255), nullable=True)
    full_name = Column(String(255), nullable=True)
    oauth_provider = Column(String(50), nullable=True)
    oauth_subject = Column(String(255), nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True, onupdate=func.now())

    driver_profiles = relationship(
        "DriverProfile",
        back_populates="user",
        cascade="all, delete-orphan",
    )

    vehicles = relationship(
        "Vehicle",
        back_populates="user",
        cascade="all, delete-orphan",
    )

    trips = relationship(
        "Trip",
        back_populates="user",
        cascade="all, delete-orphan",
    )

    loyalty_account = relationship(
        "LoyaltyAccount",
        back_populates="user",
        uselist=False,
    )

    assistant_requests = relationship(
        "AssistantRequest",
        back_populates="user",
    )

    recommendation_events = relationship(
        "RecommendationEvent",
        back_populates="user",
    )

    __table_args__ = (
        UniqueConstraint(
            "oauth_provider",
            "oauth_subject",
            name="uq_users_oauth_provider_subject",
        ),
    )

    def __repr__(self) -> str:
        return f"<User id={self.id} email={self.email}>"



# ---------------------------------------------------------------------------
# 2. DRIVER PROFILES
# ---------------------------------------------------------------------------

class DriverProfile(Base):
    __tablename__ = "driver_profiles"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)

    profile_name = Column(String(100), nullable=True)
    profile_type = Column(String(50), nullable=False, default=ProfileType.balanced.value)

    preferred_amenities = Column(JSONB, nullable=True)
    preferred_brands = Column(JSONB, nullable=True)

    avoid_tolls = Column(Boolean, nullable=False, default=False)
    avoid_highways = Column(Boolean, nullable=False, default=False)

    max_detour_minutes = Column(Integer, nullable=True)
    break_frequency_minutes = Column(Integer, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True, onupdate=func.now())

    user = relationship("User", back_populates="driver_profiles")

    trips = relationship(
        "Trip",
        back_populates="driver_profile",
    )

    def __repr__(self) -> str:
        return f"<DriverProfile id={self.id} type={self.profile_type}>"



# ---------------------------------------------------------------------------
# 3. VEHICLES
# ---------------------------------------------------------------------------

class Vehicle(Base):
    __tablename__ = "vehicles"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)

    vin = Column(String(50), nullable=True, unique=True)
    model = Column(String(100), nullable=True)
    year = Column(Integer, nullable=True)

    powertrain = Column(String(30), nullable=False)

    connector_type = Column(String(50), nullable=True)
    battery_capacity_kwh = Column(Numeric, nullable=True)
    fuel_tank_liters = Column(Numeric, nullable=True)

    consumption_kwh_per_100km = Column(Numeric, nullable=True)
    consumption_l_per_100km = Column(Numeric, nullable=True)

    service_interval_km = Column(Integer, nullable=True)
    service_interval_months = Column(Integer, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True, onupdate=func.now())

    user = relationship("User", back_populates="vehicles")

    state_snapshots = relationship(
        "VehicleStateSnapshot",
        back_populates="vehicle",
        cascade="all, delete-orphan",
    )

    trips = relationship(
        "Trip",
        back_populates="vehicle",
    )

    service_alerts = relationship(
        "ServiceAlert",
        back_populates="vehicle",
    )

    def __repr__(self) -> str:
        return f"<Vehicle id={self.id} powertrain={self.powertrain}>"



# ---------------------------------------------------------------------------
# 4. VEHICLE STATE SNAPSHOTS
# ---------------------------------------------------------------------------

class VehicleStateSnapshot(Base):
    __tablename__ = "vehicle_state_snapshots"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    vehicle_id = Column(UUID(as_uuid=True), ForeignKey("vehicles.id"), nullable=False, index=True)

    battery_soc_percent = Column(Numeric, nullable=True)
    fuel_level_percent = Column(Numeric, nullable=True)
    estimated_range_km = Column(Numeric, nullable=True)
    odometer_km = Column(Numeric, nullable=True)

    tire_pressure_status = Column(String(50), nullable=True)

    captured_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    vehicle = relationship("Vehicle", back_populates="state_snapshots")

    def __repr__(self) -> str:
        return f"<VehicleStateSnapshot id={self.id} vehicle_id={self.vehicle_id}>"



# ---------------------------------------------------------------------------
# 5. TRIPS
# ---------------------------------------------------------------------------

class Trip(Base):
    __tablename__ = "trips"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)
    vehicle_id = Column(UUID(as_uuid=True), ForeignKey("vehicles.id"), nullable=False, index=True)
    driver_profile_id = Column(UUID(as_uuid=True), ForeignKey("driver_profiles.id"), nullable=True, index=True)

    origin_label = Column(String(255), nullable=True)
    origin_lat = Column(Numeric, nullable=True)
    origin_lng = Column(Numeric, nullable=True)

    destination_label = Column(String(255), nullable=True)
    destination_lat = Column(Numeric, nullable=True)
    destination_lng = Column(Numeric, nullable=True)

    departure_time = Column(DateTime(timezone=True), nullable=True)
    requested_mode = Column(String(50), nullable=True)

    status = Column(String(50), nullable=False, default=TripStatus.planned.value)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    user = relationship("User", back_populates="trips")
    vehicle = relationship("Vehicle", back_populates="trips")
    driver_profile = relationship("DriverProfile", back_populates="trips")

    route_options = relationship(
        "RouteOption",
        back_populates="trip",
        cascade="all, delete-orphan",
    )

    assistant_requests = relationship(
        "AssistantRequest",
        back_populates="trip",
    )

    recommendations = relationship(
        "Recommendation",
        back_populates="trip",
    )

    service_alerts = relationship(
        "ServiceAlert",
        back_populates="trip",
    )

    recommendation_events = relationship(
        "RecommendationEvent",
        back_populates="trip",
    )

    def __repr__(self) -> str:
        return f"<Trip id={self.id} origin={self.origin_label} destination={self.destination_label}>"



# ---------------------------------------------------------------------------
# 6. LOCATIONS
# ---------------------------------------------------------------------------

class Location(Base):
    __tablename__ = "locations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    name = Column(String(255), nullable=False)
    location_type = Column(String(50), nullable=False, index=True)

    address = Column(Text, nullable=True)
    city = Column(String(100), nullable=True)
    country = Column(String(100), nullable=True)

    latitude = Column(Numeric, nullable=False)
    longitude = Column(Numeric, nullable=False)

    # For V1, keep this as nullable text unless you add GeoAlchemy2.
    # In pure PostgreSQL/PostGIS SQL, this should be GEOGRAPHY(Point, 4326).
    geo = Column(Text, nullable=True)

    opening_hours = Column(JSONB, nullable=True)
    rating = Column(Numeric, nullable=True)
    amenities = Column(JSONB, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True, onupdate=func.now())

    charging_station = relationship(
        "ChargingStation",
        back_populates="location",
        uselist=False,
        cascade="all, delete-orphan",
    )

    fuel_station = relationship(
        "FuelStation",
        back_populates="location",
        uselist=False,
        cascade="all, delete-orphan",
    )

    service_center = relationship(
        "ServiceCenter",
        back_populates="location",
        uselist=False,
        cascade="all, delete-orphan",
    )

    route_stops = relationship(
        "RouteStop",
        back_populates="location",
    )

    partner_locations = relationship(
        "PartnerLocation",
        back_populates="location",
    )

    recommendation_items = relationship(
        "RecommendationItem",
        back_populates="location",
    )

    def __repr__(self) -> str:
        return f"<Location id={self.id} name={self.name} type={self.location_type}>"



# ---------------------------------------------------------------------------
# 7. CHARGING STATIONS
# ---------------------------------------------------------------------------

class ChargingStation(Base):
    __tablename__ = "charging_stations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    location_id = Column(UUID(as_uuid=True), ForeignKey("locations.id"), nullable=False, unique=True, index=True)

    connector_types = Column(JSONB, nullable=True)
    max_power_kw = Column(Numeric, nullable=True)
    price_per_kwh = Column(Numeric, nullable=True)

    availability_status = Column(String(50), nullable=False, default=AvailabilityStatus.unknown.value)
    reliability_score = Column(Numeric, nullable=True)

    location = relationship("Location", back_populates="charging_station")

    def __repr__(self) -> str:
        return f"<ChargingStation id={self.id} location_id={self.location_id}>"



# ---------------------------------------------------------------------------
# 8. FUEL STATIONS
# ---------------------------------------------------------------------------

class FuelStation(Base):
    __tablename__ = "fuel_stations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    location_id = Column(UUID(as_uuid=True), ForeignKey("locations.id"), nullable=False, unique=True, index=True)

    fuel_types = Column(JSONB, nullable=True)
    price_per_liter = Column(Numeric, nullable=True)

    availability_status = Column(String(50), nullable=False, default=AvailabilityStatus.unknown.value)

    location = relationship("Location", back_populates="fuel_station")

    def __repr__(self) -> str:
        return f"<FuelStation id={self.id} location_id={self.location_id}>"



# ---------------------------------------------------------------------------
# 9. SERVICE CENTERS
# ---------------------------------------------------------------------------

class ServiceCenter(Base):
    __tablename__ = "service_centers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    location_id = Column(UUID(as_uuid=True), ForeignKey("locations.id"), nullable=False, unique=True, index=True)

    supported_brands = Column(JSONB, nullable=True)
    service_types = Column(JSONB, nullable=True)

    is_authorized = Column(Boolean, nullable=False, default=False)
    booking_url = Column(Text, nullable=True)

    availability_status = Column(String(50), nullable=False, default=AvailabilityStatus.unknown.value)

    location = relationship("Location", back_populates="service_center")

    service_alerts = relationship(
        "ServiceAlert",
        back_populates="recommended_service_center",
    )

    def __repr__(self) -> str:
        return f"<ServiceCenter id={self.id} location_id={self.location_id}>"



# ---------------------------------------------------------------------------
# 10. PARTNERS
# ---------------------------------------------------------------------------

class Partner(Base):
    __tablename__ = "partners"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    name = Column(String(255), nullable=False)
    partner_type = Column(String(50), nullable=False)

    priority_level = Column(Integer, nullable=True)
    commission_model = Column(String(50), nullable=False, default=CommissionModel.none.value)

    active = Column(Boolean, nullable=False, default=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    partner_locations = relationship(
        "PartnerLocation",
        back_populates="partner",
        cascade="all, delete-orphan",
    )

    partner_offers = relationship(
        "PartnerOffer",
        back_populates="partner",
        cascade="all, delete-orphan",
    )

    recommendation_events = relationship(
        "RecommendationEvent",
        back_populates="partner",
    )

    def __repr__(self) -> str:
        return f"<Partner id={self.id} name={self.name} type={self.partner_type}>"



# ---------------------------------------------------------------------------
# 11. PARTNER LOCATIONS
# ---------------------------------------------------------------------------

class PartnerLocation(Base):
    __tablename__ = "partner_locations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    partner_id = Column(UUID(as_uuid=True), ForeignKey("partners.id"), nullable=False, index=True)
    location_id = Column(UUID(as_uuid=True), ForeignKey("locations.id"), nullable=False, index=True)

    active = Column(Boolean, nullable=False, default=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    partner = relationship("Partner", back_populates="partner_locations")
    location = relationship("Location", back_populates="partner_locations")

    partner_offers = relationship(
        "PartnerOffer",
        back_populates="partner_location",
    )

    __table_args__ = (
        UniqueConstraint("partner_id", "location_id", name="uq_partner_location"),
    )

    def __repr__(self) -> str:
        return f"<PartnerLocation partner_id={self.partner_id} location_id={self.location_id}>"



# ---------------------------------------------------------------------------
# 12. PARTNER OFFERS
# ---------------------------------------------------------------------------

class PartnerOffer(Base):
    __tablename__ = "partner_offers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    partner_id = Column(UUID(as_uuid=True), ForeignKey("partners.id"), nullable=False, index=True)
    partner_location_id = Column(UUID(as_uuid=True), ForeignKey("partner_locations.id"), nullable=True, index=True)

    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)

    offer_type = Column(String(50), nullable=False)

    discount_percent = Column(Numeric, nullable=True)
    fixed_discount_amount = Column(Numeric, nullable=True)
    loyalty_points = Column(Integer, nullable=True)

    commission_value = Column(Numeric, nullable=True)
    campaign_priority = Column(Integer, nullable=True)

    valid_from = Column(DateTime(timezone=True), nullable=True)
    valid_until = Column(DateTime(timezone=True), nullable=True)

    active = Column(Boolean, nullable=False, default=True)

    partner = relationship("Partner", back_populates="partner_offers")
    partner_location = relationship("PartnerLocation", back_populates="partner_offers")

    recommendation_items = relationship(
        "RecommendationItem",
        back_populates="partner_offer",
    )

    loyalty_transactions = relationship(
        "LoyaltyTransaction",
        back_populates="partner_offer",
    )

    def __repr__(self) -> str:
        return f"<PartnerOffer id={self.id} title={self.title}>"

# ---------------------------------------------------------------------------
# 13. ROUTE OPTIONS
# ---------------------------------------------------------------------------

class RouteOption(Base):
    __tablename__ = "route_options"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    trip_id = Column(UUID(as_uuid=True), ForeignKey("trips.id"), nullable=False, index=True)

    provider = Column(String(50), nullable=True)
    route_name = Column(String(255), nullable=True)

    polyline = Column(Text, nullable=True)

    distance_km = Column(Numeric, nullable=True)
    duration_minutes = Column(Numeric, nullable=True)
    traffic_duration_minutes = Column(Numeric, nullable=True)

    estimated_energy_kwh = Column(Numeric, nullable=True)
    estimated_fuel_liters = Column(Numeric, nullable=True)
    estimated_cost = Column(Numeric, nullable=True)
    toll_cost = Column(Numeric, nullable=True)

    route_score = Column(Numeric, nullable=True)
    is_selected = Column(Boolean, nullable=False, default=False)

    scoring_breakdown = Column(JSONB, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    trip = relationship("Trip", back_populates="route_options")

    route_stops = relationship(
        "RouteStop",
        back_populates="route_option",
        cascade="all, delete-orphan",
    )

    recommendations = relationship(
        "Recommendation",
        back_populates="selected_route_option",
        foreign_keys="Recommendation.selected_route_option_id",
    )

    recommendation_items = relationship(
        "RecommendationItem",
        back_populates="route_option",
    )

    def __repr__(self) -> str:
        return f"<RouteOption id={self.id} trip_id={self.trip_id} score={self.route_score}>"



# ---------------------------------------------------------------------------
# 14. ROUTE STOPS
# ---------------------------------------------------------------------------

class RouteStop(Base):
    __tablename__ = "route_stops"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    route_option_id = Column(UUID(as_uuid=True), ForeignKey("route_options.id"), nullable=False, index=True)
    location_id = Column(UUID(as_uuid=True), ForeignKey("locations.id"), nullable=False, index=True)

    stop_type = Column(String(50), nullable=False)
    sequence_number = Column(Integer, nullable=False)

    detour_minutes = Column(Numeric, nullable=True)
    detour_distance_km = Column(Numeric, nullable=True)

    planned_arrival_time = Column(DateTime(timezone=True), nullable=True)
    planned_duration_minutes = Column(Numeric, nullable=True)

    reason = Column(Text, nullable=True)

    route_option = relationship("RouteOption", back_populates="route_stops")
    location = relationship("Location", back_populates="route_stops")

    def __repr__(self) -> str:
        return (
            f"<RouteStop id={self.id} route_option_id={self.route_option_id} "
            f"location_id={self.location_id} sequence={self.sequence_number}>"
        )



# ---------------------------------------------------------------------------
# 15. SERVICE ALERTS
# ---------------------------------------------------------------------------

class ServiceAlert(Base):
    __tablename__ = "service_alerts"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    vehicle_id = Column(UUID(as_uuid=True), ForeignKey("vehicles.id"), nullable=False, index=True)
    trip_id = Column(UUID(as_uuid=True), ForeignKey("trips.id"), nullable=True, index=True)

    alert_type = Column(String(50), nullable=True)
    urgency = Column(String(30), nullable=False)

    odometer_km = Column(Numeric, nullable=True)
    km_until_service = Column(Numeric, nullable=True)

    message = Column(Text, nullable=True)

    recommended_service_center_id = Column(
        UUID(as_uuid=True),
        ForeignKey("service_centers.id"),
        nullable=True,
        index=True,
    )

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    resolved_at = Column(DateTime(timezone=True), nullable=True)

    vehicle = relationship("Vehicle", back_populates="service_alerts")
    trip = relationship("Trip", back_populates="service_alerts")

    recommended_service_center = relationship(
        "ServiceCenter",
        back_populates="service_alerts",
    )

    def __repr__(self) -> str:
        return f"<ServiceAlert id={self.id} urgency={self.urgency} vehicle_id={self.vehicle_id}>"



# ---------------------------------------------------------------------------
# 16. ASSISTANT REQUESTS
# ---------------------------------------------------------------------------

class AssistantRequest(Base):
    __tablename__ = "assistant_requests"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)
    trip_id = Column(UUID(as_uuid=True), ForeignKey("trips.id"), nullable=True, index=True)

    input_type = Column(String(30), nullable=False)
    raw_input = Column(Text, nullable=False)

    extracted_intent = Column(String(100), nullable=True)
    extracted_preferences = Column(JSONB, nullable=True)

    response_text = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    user = relationship("User", back_populates="assistant_requests")
    trip = relationship("Trip", back_populates="assistant_requests")

    recommendations = relationship(
        "Recommendation",
        back_populates="assistant_request",
    )

    def __repr__(self) -> str:
        return f"<AssistantRequest id={self.id} user_id={self.user_id} input_type={self.input_type}>"



# ---------------------------------------------------------------------------
# 17. RECOMMENDATIONS
# ---------------------------------------------------------------------------

class Recommendation(Base):
    __tablename__ = "recommendations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    trip_id = Column(UUID(as_uuid=True), ForeignKey("trips.id"), nullable=False, index=True)
    assistant_request_id = Column(
        UUID(as_uuid=True),
        ForeignKey("assistant_requests.id"),
        nullable=True,
        index=True,
    )

    recommendation_type = Column(String(50), nullable=False)

    selected_route_option_id = Column(
        UUID(as_uuid=True),
        ForeignKey("route_options.id"),
        nullable=True,
        index=True,
    )

    final_score = Column(Numeric, nullable=True)
    explanation = Column(Text, nullable=True)
    scoring_breakdown = Column(JSONB, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    trip = relationship("Trip", back_populates="recommendations")

    assistant_request = relationship(
        "AssistantRequest",
        back_populates="recommendations",
    )

    selected_route_option = relationship(
        "RouteOption",
        back_populates="recommendations",
        foreign_keys=[selected_route_option_id],
    )

    recommendation_items = relationship(
        "RecommendationItem",
        back_populates="recommendation",
        cascade="all, delete-orphan",
    )

    recommendation_events = relationship(
        "RecommendationEvent",
        back_populates="recommendation",
    )

    def __repr__(self) -> str:
        return (
            f"<Recommendation id={self.id} type={self.recommendation_type} "
            f"trip_id={self.trip_id} score={self.final_score}>"
        )



# ---------------------------------------------------------------------------
# 18. RECOMMENDATION ITEMS
# ---------------------------------------------------------------------------

class RecommendationItem(Base):
    __tablename__ = "recommendation_items"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    recommendation_id = Column(
        UUID(as_uuid=True),
        ForeignKey("recommendations.id"),
        nullable=False,
        index=True,
    )

    route_option_id = Column(UUID(as_uuid=True), ForeignKey("route_options.id"), nullable=True, index=True)
    location_id = Column(UUID(as_uuid=True), ForeignKey("locations.id"), nullable=True, index=True)
    partner_offer_id = Column(UUID(as_uuid=True), ForeignKey("partner_offers.id"), nullable=True, index=True)

    item_type = Column(String(50), nullable=False)

    rank_position = Column(Integer, nullable=True)
    score = Column(Numeric, nullable=True)

    driver_value_score = Column(Numeric, nullable=True)
    partner_boost_score = Column(Numeric, nullable=True)
    loyalty_score = Column(Numeric, nullable=True)

    reason = Column(Text, nullable=True)
    accepted = Column(Boolean, nullable=True)

    recommendation = relationship("Recommendation", back_populates="recommendation_items")
    route_option = relationship("RouteOption", back_populates="recommendation_items")
    location = relationship("Location", back_populates="recommendation_items")
    partner_offer = relationship("PartnerOffer", back_populates="recommendation_items")

    loyalty_transactions = relationship(
        "LoyaltyTransaction",
        back_populates="recommendation_item",
    )

    recommendation_events = relationship(
        "RecommendationEvent",
        back_populates="recommendation_item",
    )

    def __repr__(self) -> str:
        return (
            f"<RecommendationItem id={self.id} type={self.item_type} "
            f"rank={self.rank_position} score={self.score}>"
        )



# ---------------------------------------------------------------------------
# 19. LOYALTY ACCOUNTS
# ---------------------------------------------------------------------------

class LoyaltyAccount(Base):
    __tablename__ = "loyalty_accounts"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, unique=True, index=True)

    points_balance = Column(Integer, nullable=False, default=0)
    tier = Column(String(50), nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True, onupdate=func.now())

    user = relationship("User", back_populates="loyalty_account")

    loyalty_transactions = relationship(
        "LoyaltyTransaction",
        back_populates="loyalty_account",
        cascade="all, delete-orphan",
    )

    def __repr__(self) -> str:
        return f"<LoyaltyAccount id={self.id} user_id={self.user_id} balance={self.points_balance}>"



# ---------------------------------------------------------------------------
# 20. LOYALTY TRANSACTIONS
# ---------------------------------------------------------------------------

class LoyaltyTransaction(Base):
    __tablename__ = "loyalty_transactions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    loyalty_account_id = Column(
        UUID(as_uuid=True),
        ForeignKey("loyalty_accounts.id"),
        nullable=False,
        index=True,
    )

    partner_offer_id = Column(UUID(as_uuid=True), ForeignKey("partner_offers.id"), nullable=True, index=True)
    recommendation_item_id = Column(
        UUID(as_uuid=True),
        ForeignKey("recommendation_items.id"),
        nullable=True,
        index=True,
    )

    transaction_type = Column(String(50), nullable=False)
    points = Column(Integer, nullable=False)

    description = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    loyalty_account = relationship("LoyaltyAccount", back_populates="loyalty_transactions")
    partner_offer = relationship("PartnerOffer", back_populates="loyalty_transactions")
    recommendation_item = relationship("RecommendationItem", back_populates="loyalty_transactions")

    def __repr__(self) -> str:
        return (
            f"<LoyaltyTransaction id={self.id} type={self.transaction_type} "
            f"points={self.points}>"
        )



# ---------------------------------------------------------------------------
# 21. RECOMMENDATION EVENTS
# ---------------------------------------------------------------------------

class RecommendationEvent(Base):
    __tablename__ = "recommendation_events"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False, index=True)
    trip_id = Column(UUID(as_uuid=True), ForeignKey("trips.id"), nullable=True, index=True)

    recommendation_id = Column(
        UUID(as_uuid=True),
        ForeignKey("recommendations.id"),
        nullable=True,
        index=True,
    )

    recommendation_item_id = Column(
        UUID(as_uuid=True),
        ForeignKey("recommendation_items.id"),
        nullable=True,
        index=True,
    )

    partner_id = Column(UUID(as_uuid=True), ForeignKey("partners.id"), nullable=True, index=True)

    event_type = Column(String(100), nullable=False, index=True)
    event_metadata = Column("metadata", JSONB, nullable=True)   

    created_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    user = relationship("User", back_populates="recommendation_events")
    trip = relationship("Trip", back_populates="recommendation_events")
    recommendation = relationship("Recommendation", back_populates="recommendation_events")
    recommendation_item = relationship("RecommendationItem", back_populates="recommendation_events")
    partner = relationship("Partner", back_populates="recommendation_events")

    def __repr__(self) -> str:
        return f"<RecommendationEvent id={self.id} type={self.event_type}>"

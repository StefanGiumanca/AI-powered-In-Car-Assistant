from pydantic import BaseModel, Field


class QueryInterpretation(BaseModel):
    intent: str = Field(..., description="find_stop, add_destination, service_request, unknown")
    place_category: str = Field(..., description="fuel, charging, restaurant, cafe, hotel, service, parking, unknown")
    google_place_type: str | None = None

    brand_constraint: str | None = None
    food_query: str | None = None

    strict_brand: bool = False
    radius_meters: int = 5000

    original_query: str


class RecommendationQueryRequest(BaseModel):
    user_id: str
    vehicle_id: str
    driver_profile_id: str
    query: str

    latitude: float
    longitude: float

    trip_id: str | None = None


class RecommendationCandidateResponse(BaseModel):
    google_place_id: str
    name: str
    address: str | None = None
    latitude: float
    longitude: float
    rating: float | None = None
    score: float
    reason: str
    partner_name: str | None = None
    offer_title: str | None = None
    loyalty_points: int | None = None
    matches_requested_brand: bool


class RecommendationQueryResponse(BaseModel):
    interpretation: QueryInterpretation
    radius_meters: int
    strict_match_found: bool
    message: str
    candidates: list[RecommendationCandidateResponse]
    can_expand_radius: bool = True
    next_radius_meters: int | None = 10000

class AcceptRecommendationRequest(BaseModel):
    user_id: str
    trip_id: str | None = None
    google_place_id: str
    name: str
    address: str | None = None
    latitude: float
    longitude: float
    location_type: str
    reason: str | None = None
    score: float | None = None
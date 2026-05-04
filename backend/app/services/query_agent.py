import re

from app.schemas.query_agent import QueryInterpretation


FUEL_BRANDS = [
    "petrom",
    "omv",
    "mol",
    "rompetrol",
    "lukoil",
    "shell",
]

FOOD_KEYWORDS = {
    "shaorma": "shaorma",
    "shawarma": "shaorma",
    "burger": "burger",
    "pizza": "pizza",
    "coffee": "coffee",
    "cafe": "coffee",
    "restaurant": "restaurant",
    "mcdonald": "McDonald's",
    "kfc": "KFC",
    "starbucks": "Starbucks",
}

PLACE_TYPE_BY_CATEGORY = {
    "fuel": "gas_station",
    "charging": "electric_vehicle_charging_station",
    "restaurant": "restaurant",
    "cafe": "cafe",
    "hotel": "hotel",
    "service": "car_repair",
    "parking": "parking",
}


def interpret_driver_query(query: str) -> QueryInterpretation:
    normalized = query.lower().strip()

    # Fuel / refill intent
    if any(word in normalized for word in ["refill", "fuel", "gas", "petrol", "diesel", "benzina", "motorina", "alimentez"]):
        brand = extract_brand(normalized, FUEL_BRANDS)

        return QueryInterpretation(
            intent="find_stop",
            place_category="fuel",
            google_place_type=PLACE_TYPE_BY_CATEGORY["fuel"],
            brand_constraint=brand,
            strict_brand=brand is not None,
            radius_meters=5000,
            original_query=query,
        )

    # EV charging intent
    if any(word in normalized for word in ["charge", "charging", "charger", "incarc", "stație de încărcare", "statie de incarcare"]):
        return QueryInterpretation(
            intent="find_stop",
            place_category="charging",
            google_place_type=PLACE_TYPE_BY_CATEGORY["charging"],
            brand_constraint=None,
            strict_brand=False,
            radius_meters=5000,
            original_query=query,
        )

    # Food / restaurant intent
    for keyword, food_query in FOOD_KEYWORDS.items():
        if keyword in normalized:
            category = "cafe" if food_query in {"coffee", "Starbucks"} else "restaurant"

            return QueryInterpretation(
                intent="find_stop",
                place_category=category,
                google_place_type=PLACE_TYPE_BY_CATEGORY[category],
                brand_constraint=food_query if food_query in {"McDonald's", "KFC", "Starbucks"} else None,
                food_query=food_query,
                strict_brand=food_query in {"McDonald's", "KFC", "Starbucks"},
                radius_meters=5000,
                original_query=query,
            )

    # Hotel intent
    if any(word in normalized for word in ["hotel", "sleep", "stay", "cazare"]):
        return QueryInterpretation(
            intent="find_stop",
            place_category="hotel",
            google_place_type=PLACE_TYPE_BY_CATEGORY["hotel"],
            radius_meters=10000,
            original_query=query,
        )

    # Service intent
    if any(word in normalized for word in ["service", "repair", "mechanic", "revizie", "problema masina"]):
        return QueryInterpretation(
            intent="service_request",
            place_category="service",
            google_place_type=PLACE_TYPE_BY_CATEGORY["service"],
            radius_meters=10000,
            original_query=query,
        )

    return QueryInterpretation(
        intent="unknown",
        place_category="unknown",
        google_place_type=None,
        radius_meters=5000,
        original_query=query,
    )


def extract_brand(normalized_query: str, known_brands: list[str]) -> str | None:
    for brand in known_brands:
        pattern = rf"\b{re.escape(brand)}\b"
        if re.search(pattern, normalized_query):
            return brand.upper()

    return None
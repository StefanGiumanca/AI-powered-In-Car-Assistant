import re
import os
from typing import Literal

from pydantic import BaseModel, Field

from app.schemas.google_place_types import GooglePlaceType
from app.schemas.query_agent import QueryInterpretation
from app.services.place_ranking_service import PlaceSearchIntent, SearchStrategy


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

OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses"
DEFAULT_OPENAI_PLACE_INTENT_MODEL = "gpt-4.1-mini"


class AIPlaceSearchIntent(BaseModel):
    primary_query: str = Field(..., min_length=1, max_length=120)
    provider_types: list[GooglePlaceType] = Field(..., max_length=5)
    strong_positive_signals: list[str] = Field(..., max_length=10)
    positive_signals: list[str] = Field(..., max_length=15)
    negative_signals: list[str] = Field(..., max_length=15)
    excluded_types: list[GooglePlaceType] = Field(..., max_length=8)
    strictness: Literal["low", "medium", "high"]
    open_now_required: bool
    search_strategy: SearchStrategy
    requires_specific_match: bool
    requested_brand: str | None = Field(..., max_length=80)
    search_text_queries: list[str] = Field(..., min_length=1, max_length=3)


class ExtractedPlaceIntent(BaseModel):
    intent: PlaceSearchIntent
    requested_brand: str | None
    search_text_queries: list[str]


SYSTEM_PROMPT = """
You are an intent parser for a location recommendation engine.

Your job is to convert a user's natural language query into a structured JSON
search intent. The backend will use this JSON to query Google Places and rank
candidates.

Return only valid JSON. Do not return explanations. Do not invent real places.
Do not include place names unless the user explicitly asked for a brand,
manufacturer, or known chain.

Field rules:
- primary_query: short description of the user's main need.
- provider_types: Google Places types that should be used to discover candidates.
  Prefer the most specific valid Google Places type when one exists. For example,
  use "romanian_restaurant" for Romanian food, "shawarma_restaurant" for shawarma,
  and "kebab_shop" for kebab instead of only using "restaurant".
- strong_positive_signals: terms that strongly prove a candidate matches the request.
- positive_signals: secondary terms that make a candidate more relevant.
- negative_signals: terms that suggest a candidate is wrong for the request.
- excluded_types: Google Places types that should be rejected.
- strictness:
  - "high" for explicit brands, exact products, cuisines, shop subtypes, or narrow needs.
  - "medium" for clear but broader categories.
  - "low" for exploratory or vague requests.
- open_now_required: true only if the user explicitly asks for open now, tonight,
  late-night, non-stop, currently available, or similar.
- requested_brand: brand, chain, or manufacturer explicitly requested by the user.
  Use null if none.
- search_text_queries: 1 to 3 concise Google Places Text Search queries.
- search_strategy:
  - "nearby_type" for generic Google-place-type categories where type match is enough,
    such as gas station, parking, pharmacy, hospital, EV charging station, car wash.
  - "text_strict" for explicit brands, exact products, qualitative requests, or narrow
    requests, such as Petrom, Mercedes dealership, elegant clothes, cheap gas station.
  - "hybrid" when both type discovery and text matching are useful, such as shawarma
    restaurant, fast food, specific cuisine, or ambiguous requests.
- requires_specific_match:
  - false only for generic type categories where a Google type match is enough.
  - true for brands, products, qualitative requests, food items, cuisines, and narrow needs.

Language rules:
- Always return every JSON string value in English, regardless of the user's language.
- Translate the user's intent into English before filling the JSON.
- Keep brand names, chain names, manufacturer names, and proper nouns unchanged.
- search_text_queries must be optimized English Google Places queries.
- Do not include Romanian words in search_text_queries unless they are part of a
  brand name, chain name, manufacturer name, or proper noun.
- Example: if the user asks "benzinarie ieftina", return queries such as
  "cheap gas station", "low price fuel station", and "discount gas station".
"""


def build_user_prompt(user_query: str) -> str:
    return f"""
User query:
{user_query}

Important:
The user query may be Romanian or another language. Return the entire JSON in
English. Keep only explicit brands and proper nouns unchanged.

Return JSON with this exact shape:

{{
  "primary_query": "string",
  "provider_types": ["google_place_type"],
  "strong_positive_signals": ["string"],
  "positive_signals": ["string"],
  "negative_signals": ["string"],
  "excluded_types": ["google_place_type"],
  "strictness": "low | medium | high",
  "open_now_required": true,
  "search_strategy": "nearby_type | text_strict | hybrid",
  "requires_specific_match": true,
  "requested_brand": "string or null",
  "search_text_queries": ["string"]
}}

Use only valid Google Places types for provider_types and excluded_types.
"""


async def extract_place_search_intent(user_query: str) -> ExtractedPlaceIntent:
    import httpx

    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is not configured.")

    model = os.getenv("OPENAI_PLACE_INTENT_MODEL", DEFAULT_OPENAI_PLACE_INTENT_MODEL)

    async with httpx.AsyncClient(timeout=20) as client:
        response = await client.post(
            OPENAI_RESPONSES_URL,
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model,
                "input": [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": build_user_prompt(user_query)},
                ],
                "text": {
                    "format": {
                        "type": "json_schema",
                        "name": "place_search_intent",
                        "strict": True,
                        "schema": build_place_intent_json_schema(),
                    }
                },
            },
        )
        response.raise_for_status()

    parsed = AIPlaceSearchIntent.model_validate_json(
        extract_response_text(response.json())
    )

    intent = PlaceSearchIntent(
        primary_query=parsed.primary_query,
        provider_types=[place_type.value for place_type in parsed.provider_types],
        strong_positive_signals=parsed.strong_positive_signals,
        positive_signals=parsed.positive_signals,
        negative_signals=parsed.negative_signals,
        excluded_types=[place_type.value for place_type in parsed.excluded_types],
        strictness=parsed.strictness,
        open_now_required=parsed.open_now_required,
        search_strategy=parsed.search_strategy,
        requires_specific_match=parsed.requires_specific_match,
    )

    return ExtractedPlaceIntent(
        intent=intent,
        requested_brand=parsed.requested_brand,
        search_text_queries=parsed.search_text_queries,
    )


def build_place_intent_json_schema() -> dict:
    google_place_type_values = [place_type.value for place_type in GooglePlaceType]

    return {
        "type": "object",
        "additionalProperties": False,
        "required": [
            "primary_query",
            "provider_types",
            "strong_positive_signals",
            "positive_signals",
            "negative_signals",
            "excluded_types",
            "strictness",
            "open_now_required",
            "search_strategy",
            "requires_specific_match",
            "requested_brand",
            "search_text_queries",
        ],
        "properties": {
            "primary_query": {
                "type": "string",
                "minLength": 1,
                "maxLength": 120,
            },
            "provider_types": {
                "type": "array",
                "maxItems": 5,
                "items": {"type": "string", "enum": google_place_type_values},
            },
            "strong_positive_signals": {
                "type": "array",
                "maxItems": 10,
                "items": {"type": "string", "maxLength": 80},
            },
            "positive_signals": {
                "type": "array",
                "maxItems": 15,
                "items": {"type": "string", "maxLength": 80},
            },
            "negative_signals": {
                "type": "array",
                "maxItems": 15,
                "items": {"type": "string", "maxLength": 80},
            },
            "excluded_types": {
                "type": "array",
                "maxItems": 8,
                "items": {"type": "string", "enum": google_place_type_values},
            },
            "strictness": {"type": "string", "enum": ["low", "medium", "high"]},
            "open_now_required": {"type": "boolean"},
            "search_strategy": {
                "type": "string",
                "enum": ["nearby_type", "text_strict", "hybrid"],
            },
            "requires_specific_match": {"type": "boolean"},
            "requested_brand": {
                "anyOf": [
                    {"type": "string", "maxLength": 80},
                    {"type": "null"},
                ]
            },
            "search_text_queries": {
                "type": "array",
                "minItems": 1,
                "maxItems": 3,
                "items": {"type": "string", "minLength": 1, "maxLength": 120},
            },
        },
    }


def extract_response_text(response_data: dict) -> str:
    if "output_text" in response_data:
        return response_data["output_text"]

    for output_item in response_data.get("output", []):
        for content_item in output_item.get("content", []):
            text = content_item.get("text")
            if text:
                return text

    raise RuntimeError("OpenAI response did not contain output text.")


def interpret_driver_query(query: str) -> QueryInterpretation:
    normalized = query.lower().strip()

    # Fuel / refill intent
    if contains_any_word(
        normalized,
        [
            "refill",
            "fuel",
            "gas",
            "petrol",
            "diesel",
            "benzina",
            "motorina",
            "alimentez",
        ],
    ):
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


def contains_any_word(normalized_query: str, words: list[str]) -> bool:
    return any(
        re.search(rf"\b{re.escape(word)}\b", normalized_query)
        for word in words
    )

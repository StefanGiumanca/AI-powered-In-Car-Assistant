from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


# Shared DTO primitives and reusable constrained types for the API contract.
class APIModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class CurrentLocationDTO(APIModel):
    lat: float = Field(..., ge=-90, le=90)
    lng: float = Field(..., ge=-180, le=180)


class LabeledLocationDTO(APIModel):
    label: str = Field(..., min_length=1, max_length=200)
    lat: float = Field(..., ge=-90, le=90)
    lng: float = Field(..., ge=-180, le=180)


class NamedLocationDTO(APIModel):
    name: str = Field(..., min_length=1, max_length=200)
    lat: float = Field(..., ge=-90, le=90)
    lng: float = Field(..., ge=-180, le=180)


class IdentifiedLocationDTO(APIModel):
    id: str = Field(..., min_length=1, max_length=100)
    name: str = Field(..., min_length=1, max_length=200)
    lat: float = Field(..., ge=-90, le=90)
    lng: float = Field(..., ge=-180, le=180)


TripStatus = Literal["active", "completed", "cancelled"]
RecommendationPriority = Literal["low", "medium", "high"]
RequestedMode = Literal["fastest", "cheapest", "family", "balanced"]
TirePressureStatus = Literal["ok", "low", "critical", "unknown", "high"]

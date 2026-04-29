from typing import Any

from app.schemas import NextActionResponse, RecommendationDTO


trips: dict[str, dict[str, Any]] = {}
vehicle_state_snapshots: list[dict[str, Any]] = []
recommendations: dict[str, RecommendationDTO] = {}
next_actions_by_trip_id: dict[str, NextActionResponse] = {}
recommendation_events: list[dict[str, Any]] = []

_trip_sequence = 0
_recommendation_sequence = 0


def reset_mock_state() -> None:
    global _trip_sequence, _recommendation_sequence

    trips.clear()
    vehicle_state_snapshots.clear()
    recommendations.clear()
    next_actions_by_trip_id.clear()
    recommendation_events.clear()
    _trip_sequence = 0
    _recommendation_sequence = 0


def next_trip_id() -> str:
    global _trip_sequence

    _trip_sequence += 1
    return f"trip_{_trip_sequence}"


def next_recommendation_id() -> str:
    global _recommendation_sequence

    _recommendation_sequence += 1
    return f"rec_{_recommendation_sequence}"

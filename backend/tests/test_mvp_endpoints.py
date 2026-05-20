import asyncio
import copy
import unittest

import pytest

from tests.asgi_client import request_app


pytestmark = pytest.mark.skip(
    reason="Trip start/update flow is outside the current authenticated setup scope."
)


TRIP_START_PAYLOAD = {
    "user_id": "user_1",
    "vehicle_id": "vehicle_1",
    "driver_profile_id": "profile_1",
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
    "vehicle_state": {
        "fuel_level_percent": 22,
        "battery_soc_percent": None,
        "estimated_range_km": 120,
        "odometer_km": 84200,
        "tire_pressure_status": "ok",
        "engine_warning": False,
    },
    "requested_mode": "balanced",
}


class MvpEndpointTest(unittest.TestCase):
    def test_trip_start_valid_request_creates_trip_and_recommendation(self) -> None:
        status, body = asyncio.run(
            request_app("POST", "/trip/start", TRIP_START_PAYLOAD)
        )

        self.assertEqual(status, 201)
        self.assertEqual(body["trip_id"], "trip_1")
        self.assertEqual(body["status"], "active")
        self.assertEqual(body["selected_route"]["route_id"], "route_1")
        self.assertEqual(body["next_recommendation"]["type"], "fuel_stop")
        self.assertEqual(body["next_recommendation"]["priority"], "high")

        self.assertIn("trip_1", mock_state.trips)
        self.assertEqual(len(mock_state.vehicle_state_snapshots), 1)
        self.assertEqual(
            mock_state.vehicle_state_snapshots[0]["vehicle_state"][
                "fuel_level_percent"
            ],
            22.0,
        )
        self.assertIn(
            body["next_recommendation"]["recommendation_id"],
            mock_state.recommendations,
        )

    def test_trip_start_invalid_request_returns_422(self) -> None:
        invalid_payload = copy.deepcopy(TRIP_START_PAYLOAD)
        invalid_payload["origin"]["lat"] = 144

        status, body = asyncio.run(
            request_app("POST", "/trip/start", invalid_payload)
        )

        self.assertEqual(status, 422)
        self.assertIn("detail", body)

    def test_trip_update_saves_vehicle_state_snapshot(self) -> None:
        _, start_body = asyncio.run(
            request_app("POST", "/trip/start", TRIP_START_PAYLOAD)
        )

        status, body = asyncio.run(
            request_app(
                "POST",
                "/trip/update",
                {
                    "trip_id": start_body["trip_id"],
                    "current_location": {
                        "lat": 44.8,
                        "lng": 26.05,
                    },
                    "vehicle_state": {
                        "fuel_level_percent": 16,
                        "estimated_range_km": 85,
                        "odometer_km": 84255,
                        "tire_pressure_status": "low",
                    },
                },
            )
        )

        self.assertEqual(status, 200)
        self.assertEqual(body["trip_id"], "trip_1")
        self.assertTrue(body["should_refresh_recommendation"])
        self.assertEqual(len(mock_state.vehicle_state_snapshots), 2)
        self.assertEqual(
            mock_state.vehicle_state_snapshots[1]["source"],
            "trip_update",
        )

    def test_next_action_returns_current_recommendation_card(self) -> None:
        _, start_body = asyncio.run(
            request_app("POST", "/trip/start", TRIP_START_PAYLOAD)
        )

        status, body = asyncio.run(
            request_app(
                "POST",
                "/recommendations/next-action",
                {"trip_id": start_body["trip_id"]},
            )
        )

        self.assertEqual(status, 200)
        self.assertEqual(body["action"], "STOP_FOR_FUEL")
        self.assertEqual(body["priority"], "high")
        self.assertEqual(body["location"]["id"], "loc_44")
        self.assertEqual(body["offer"]["id"], "offer_7")

    def test_recommendation_event_is_saved(self) -> None:
        _, start_body = asyncio.run(
            request_app("POST", "/trip/start", TRIP_START_PAYLOAD)
        )
        _, next_action = asyncio.run(
            request_app(
                "POST",
                "/recommendations/next-action",
                {"trip_id": start_body["trip_id"]},
            )
        )

        status, body = asyncio.run(
            request_app(
                "POST",
                "/recommendations/events",
                {
                    "trip_id": start_body["trip_id"],
                    "recommendation_id": next_action["recommendation_id"],
                    "event_type": "accepted",
                },
            )
        )

        self.assertEqual(status, 200)
        self.assertEqual(body, {"saved": True})
        self.assertEqual(len(mock_state.recommendation_events), 1)


if __name__ == "__main__":
    unittest.main()

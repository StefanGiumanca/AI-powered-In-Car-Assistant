from uuid import UUID

from sqlalchemy.orm import Session

from app.db import models as dbm
from app.schemas.query_agent import AcceptRecommendationRequest
from app.services.location_cache_service import find_location_by_google_place_id


def accept_recommendation(
    db: Session,
    payload: AcceptRecommendationRequest,
):
    location = find_location_by_google_place_id(db, payload.google_place_id)

    if location is None:
        location = dbm.Location(
            name=payload.name,
            location_type=payload.location_type,
            address=payload.address,
            city=None,
            country="Romania",
            latitude=payload.latitude,
            longitude=payload.longitude,
            amenities={
                "google_place_id": payload.google_place_id,
                "accepted_from_recommendation": True,
            },
        )
        db.add(location)
        db.flush()

    recommendation = None

    if payload.trip_id:
        recommendation = dbm.Recommendation(
            trip_id=UUID(payload.trip_id),
            recommendation_type="stop",
            final_score=payload.score,
            explanation=payload.reason,
            scoring_breakdown={
                "source": "driver_query_acceptance",
                "google_place_id": payload.google_place_id,
            },
        )
        db.add(recommendation)
        db.flush()

        item = dbm.RecommendationItem(
            recommendation_id=recommendation.id,
            location_id=location.id,
            item_type=f"{payload.location_type}_stop",
            rank_position=1,
            score=payload.score,
            reason=payload.reason,
            accepted=True,
        )
        db.add(item)
        db.flush()

        event = dbm.RecommendationEvent(
            user_id=UUID(payload.user_id),
            trip_id=UUID(payload.trip_id),
            recommendation_id=recommendation.id,
            recommendation_item_id=item.id,
            event_type="accepted",
            event_metadata={
                "google_place_id": payload.google_place_id,
                "source": "driver_query_acceptance",
            },
        )
        db.add(event)

    db.commit()

    return {
        "accepted": True,
        "location_id": str(location.id),
        "recommendation_id": str(recommendation.id) if recommendation else None,
        "message": "Selected place was saved. It can now be added as a stop or destination.",
    }
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.db import models as dbm
from app.services.place_candidate_mapper import PlaceCandidate


def get_or_create_location_from_candidate(
    db: Session,
    candidate: PlaceCandidate,
) -> dbm.Location:
    existing = find_location_by_google_place_id(db, candidate.google_place_id)

    if existing:
        return existing

    location = dbm.Location(
        name=candidate.name,
        location_type=candidate.location_type,
        address=candidate.address,
        city=None,
        country="Romania",
        latitude=candidate.latitude,
        longitude=candidate.longitude,
        opening_hours=candidate.raw.get("currentOpeningHours"),
        rating=candidate.rating,
        amenities={
            "google_place_id": candidate.google_place_id,
            "google_primary_type": candidate.primary_type,
            "google_types": candidate.types,
            "google_business_status": candidate.business_status,
            "google_rating_count": candidate.rating_count,
            "last_synced_at": datetime.now(timezone.utc).isoformat(),
        },
    )

    db.add(location)
    db.flush()

    return location


def find_location_by_google_place_id(
    db: Session,
    google_place_id: str,
) -> dbm.Location | None:
    return (
        db.query(dbm.Location)
        .filter(dbm.Location.amenities["google_place_id"].astext == google_place_id)
        .first()
    )
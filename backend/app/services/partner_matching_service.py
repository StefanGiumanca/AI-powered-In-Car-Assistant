from sqlalchemy.orm import Session

from app.db import models as dbm
from app.services.place_candidate_mapper import PlaceCandidate


def find_partner_for_candidate(
    db: Session,
    candidate: PlaceCandidate,
) -> dbm.Partner | None:
    partners = (
        db.query(dbm.Partner)
        .filter(dbm.Partner.active.is_(True))
        .all()
    )

    candidate_name = candidate.name.lower()

    for partner in partners:
        if partner.name.lower() in candidate_name:
            return partner

    return None


def find_best_offer_for_partner(
    db: Session,
    partner: dbm.Partner | None,
) -> dbm.PartnerOffer | None:
    if partner is None:
        return None

    return (
        db.query(dbm.PartnerOffer)
        .filter(dbm.PartnerOffer.partner_id == partner.id)
        .filter(dbm.PartnerOffer.active.is_(True))
        .order_by(
            dbm.PartnerOffer.campaign_priority.desc().nullslast(),
            dbm.PartnerOffer.loyalty_points.desc().nullslast(),
        )
        .first()
    )
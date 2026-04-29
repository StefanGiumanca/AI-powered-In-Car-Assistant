from fastapi import HTTPException, status

from app.schemas import (
    NextActionRequest,
    NextActionResponse,
    RecommendationDTO,
    RecommendationEventRequest,
    RecommendationEventResponse,
    VehicleStateDTO,
)
from app.services import mock_state


def build_recommendation(
    recommendation_id: str,
    vehicle_state: VehicleStateDTO,
) -> RecommendationDTO:
    if vehicle_state.tire_pressure_status == "low":
        return RecommendationDTO(
            recommendation_id=recommendation_id,
            type="service_stop",
            title="Verifica presiunea in anvelope",
            reason="Presiunea este raportata ca fiind scazuta si merita verificata la urmatoarea oprire.",
            priority="high",
            location={
                "name": "Bosch Service Ploiesti",
                "lat": 44.936,
                "lng": 26.012,
            },
            detour_minutes=6,
            loyalty_points=30,
        )

    if vehicle_state.fuel_level_percent is not None and vehicle_state.fuel_level_percent < 25:
        return RecommendationDTO(
            recommendation_id=recommendation_id,
            type="fuel_stop",
            title="Opreste pentru alimentare in 28 km",
            reason="Nivelul de combustibil este scazut si exista o statie partenera cu deviatie mica.",
            priority="high",
            location={
                "name": "OMV Ploiesti",
                "lat": 44.936,
                "lng": 26.012,
            },
            detour_minutes=4,
            loyalty_points=50,
        )

    return RecommendationDTO(
        recommendation_id=recommendation_id,
        type="rest_stop",
        title="Continua traseul planificat",
        reason="Starea vehiculului este buna si nu este necesara o oprire urgenta.",
        priority="low",
        location={
            "name": "Popas DN1",
            "lat": 44.91,
            "lng": 26.02,
        },
        detour_minutes=2,
        loyalty_points=10,
    )


def build_next_action(
    recommendation_id: str,
    vehicle_state: VehicleStateDTO,
) -> NextActionResponse:
    if vehicle_state.tire_pressure_status == "low":
        return NextActionResponse(
            recommendation_id=recommendation_id,
            action="STOP_FOR_SERVICE",
            title="Verifica anvelopele la Bosch Service",
            reason="Presiunea scazuta poate afecta siguranta si consumul pe traseu.",
            priority="high",
            confidence=0.84,
            detour_minutes=6,
            location={
                "id": "loc_21",
                "name": "Bosch Service Ploiesti",
                "lat": 44.936,
                "lng": 26.012,
            },
            offer={
                "id": "offer_3",
                "title": "Verificare presiune gratuita",
                "loyalty_points": 30,
            },
        )

    if vehicle_state.fuel_level_percent is not None and vehicle_state.fuel_level_percent < 25:
        return NextActionResponse(
            recommendation_id=recommendation_id,
            action="STOP_FOR_FUEL",
            title="Alimenteaza la MOL in 12 km",
            reason="Combustibil sub 25%, deviatie mica si oferta activa.",
            priority="high",
            confidence=0.87,
            detour_minutes=3,
            location={
                "id": "loc_44",
                "name": "MOL DN1",
                "lat": 44.91,
                "lng": 26.02,
            },
            offer={
                "id": "offer_7",
                "title": "5% discount fuel",
                "loyalty_points": 40,
            },
        )

    return NextActionResponse(
        recommendation_id=recommendation_id,
        action="CONTINUE_TRIP",
        title="Continua traseul",
        reason="Nu exista o actiune urgenta pentru starea curenta a vehiculului.",
        priority="low",
        confidence=0.72,
        detour_minutes=0,
        location={
            "id": "route_current",
            "name": "Current route",
            "lat": 44.8,
            "lng": 26.05,
        },
        offer={
            "id": "offer_none",
            "title": "No active offer",
            "loyalty_points": 0,
        },
    )


def get_next_action(payload: NextActionRequest) -> NextActionResponse:
    next_action = mock_state.next_actions_by_trip_id.get(payload.trip_id)
    if next_action is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trip not found or no recommendation is available.",
        )
    return next_action


def save_recommendation_event(
    payload: RecommendationEventRequest,
) -> RecommendationEventResponse:
    if payload.trip_id not in mock_state.trips:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Trip not found.",
        )

    if payload.recommendation_id not in mock_state.recommendations:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Recommendation not found.",
        )

    mock_state.recommendation_events.append(payload.model_dump())
    return RecommendationEventResponse(saved=True)

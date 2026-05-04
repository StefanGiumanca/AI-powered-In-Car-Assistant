from sqlalchemy.orm import Session

from app.db.models import DriverProfile, User
from app.schemas import DriverProfileCreateRequest, DriverProfileResponse


def driver_profile_to_response(
    driver_profile: DriverProfile,
) -> DriverProfileResponse:
    return DriverProfileResponse(
        id=str(driver_profile.id),
        profile_name=driver_profile.profile_name,
        profile_type=driver_profile.profile_type,
        preferred_amenities=driver_profile.preferred_amenities or [],
        preferred_brands=driver_profile.preferred_brands or [],
        avoid_tolls=driver_profile.avoid_tolls,
        avoid_highways=driver_profile.avoid_highways,
        max_detour_minutes=driver_profile.max_detour_minutes or 10,
        break_frequency_minutes=driver_profile.break_frequency_minutes or 120,
    )


def create_driver_profile(
    db: Session,
    current_user: User,
    payload: DriverProfileCreateRequest,
) -> DriverProfileResponse:
    driver_profile = DriverProfile(
        user_id=current_user.id,
        profile_name=payload.profile_name,
        profile_type=payload.profile_type,
        preferred_amenities=payload.preferred_amenities,
        preferred_brands=payload.preferred_brands,
        avoid_tolls=payload.avoid_tolls,
        avoid_highways=payload.avoid_highways,
        max_detour_minutes=payload.max_detour_minutes,
        break_frequency_minutes=payload.break_frequency_minutes,
    )

    db.add(driver_profile)
    db.commit()
    db.refresh(driver_profile)

    return driver_profile_to_response(driver_profile)

from typing import Literal

from .common import APIModel


# DTO for the health endpoint response.
class HealthResponse(APIModel):
    status: Literal["ok"]

import os
from pathlib import Path

from dotenv import load_dotenv
from pydantic import BaseModel


load_dotenv(Path(__file__).resolve().parents[3] / ".env")


class Settings(BaseModel):
    app_name: str = "DavaRoutes API"
    jwt_secret_key: str = os.environ["JWT_SECRET_KEY"]
    jwt_algorithm: str = os.getenv("JWT_ALGORITHM", "HS256")
    access_token_expire_hours: int = int(
        os.getenv("ACCESS_TOKEN_EXPIRE_HOURS", "24")
    )
    google_oauth_client_id: str | None = os.getenv("GOOGLE_OAUTH_CLIENT_ID")


settings = Settings()

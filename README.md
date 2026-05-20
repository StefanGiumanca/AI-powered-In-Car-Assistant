# AI-powered-In-Car-Assistant

# DavaRoutes

Android navigation assistant with a FastAPI backend for route planning, vehicle profiles, trip history, and contextual stop recommendations.

## Table of Contents

- [About](#about)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Environment Variables](#environment-variables)
- [Run with Docker](#run-with-docker)
- [Database Migrations](#database-migrations)
- [Seed Data](#seed-data)
- [Backend Development](#backend-development)
- [Android App](#android-app)
- [Testing](#testing)
- [API Areas](#api-areas)
- [Troubleshooting](#troubleshooting)

## About

DavaRoutes helps drivers plan trips, manage driver and vehicle profiles, preview routes, and receive contextual recommendations for charging, fuel, food, service stops, and partner offers.

The repository contains a native Android client, a FastAPI backend, a PostGIS database setup, and local development tooling through Docker Compose.

## Features

- Email/password authentication
- Google OAuth authentication
- Driver profile setup
- Vehicle profile setup
- Trip creation
- Recent trip history
- Google route previews
- Google Places search and autocomplete
- Charging, fuel, food, service, and partner recommendations
- Vehicle state tracking
- Seeded local development dataset

## Tech Stack

### Backend

| Area | Technology |
| --- | --- |
| API | FastAPI |
| Language | Python 3.13 |
| Database | PostgreSQL + PostGIS |
| ORM | SQLAlchemy |
| Migrations | Alembic |
| Auth | JWT, bcrypt, Google OAuth |
| External APIs | Google Routes, Google Places, OpenAI Responses API |

### Android

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Networking | Retrofit, Gson |
| Maps | Google Maps Compose |
| Navigation | Google Navigation SDK |
| Places | Google Places |
| Auth | Google Sign-In |

## Project Structure

```text
.
├── android/                 # Kotlin Android app
├── backend/
│   ├── app/
│   │   ├── api/             # FastAPI routers and dependencies
│   │   ├── core/            # Config and security
│   │   ├── db/              # Database session, models, seed entrypoint
│   │   ├── schemas/         # Pydantic schemas
│   │   └── services/        # Business logic and third-party integrations
│   ├── alembic/             # Database migrations
│   └── tests/               # Backend test suite
├── db/
│   └── init/                # PostGIS extension initialization
└── docker-compose.yml       # Local backend + database stack
```

## Prerequisites

- Docker and Docker Compose
- Android Studio
- Python 3.13, if running the backend outside Docker
- JDK compatible with the Android Gradle Plugin
- Google Cloud project with Maps, Routes, Places, and OAuth credentials
- OpenAI API key, if using the AI place-query flow

## Environment Variables

Create a root `.env` file. For local Docker development, the default database settings match `docker-compose.yml`.

```env
POSTGRES_USER=davaroutes_user
POSTGRES_PASSWORD=davaroutes_password
POSTGRES_DB=davaroutes_db
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

DATABASE_URL=postgresql+psycopg2://davaroutes_user:davaroutes_password@postgres:5432/davaroutes_db
LOCAL_DATABASE_URL=postgresql+psycopg2://davaroutes_user:davaroutes_password@localhost:5432/davaroutes_db

JWT_SECRET_KEY=replace-with-a-long-random-secret
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_HOURS=24

GOOGLE_MAPS_API_KEY=replace-with-google-api-key
GOOGLE_OAUTH_CLIENT_ID=replace-with-web-oauth-client-id

OPENAI_API_KEY=replace-with-openai-api-key
OPENAI_PLACE_INTENT_MODEL=gpt-4.1-mini
```

Optional backend environment variables:

- `GOOGLE_PLACES_BASE_URL`: override the Google Places API base URL for testing.
- `BACKEND_PORT`: currently present in local env examples, but the compose file exposes the backend on `8000`.

For Android, create `android/local.properties` and add:

```properties
google_maps_api_key=replace-with-google-api-key
google_web_client_id=replace-with-web-oauth-client-id
```

The Android OAuth client in Google Cloud must use package name `com.example.davaroutes` and the SHA-1 fingerprint for the keystore you run with.

## Run with Docker

From the repository root:

```bash
docker compose up --build
```

The backend will be available at:

- API root: `http://localhost:8000/`
- Health check: `http://localhost:8000/health`
- Swagger docs: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

To stop the stack:

```bash
docker compose down
```

To remove the database volume and start fresh:

```bash
docker compose down -v
```

## Database Migrations

When running through Docker:

```bash
docker compose exec backend alembic upgrade head
```

When running locally from `backend/`, make sure `LOCAL_DATABASE_URL` points at your local database, then run:

```bash
alembic upgrade head
```

## Seed Data

After the backend and database are running, insert sample users, driver profiles, vehicles, locations, partner offers, and loyalty accounts:

```bash
docker compose exec backend python -m app.db.seed
```

To inspect the database:

```bash
docker exec -it davaroutes_postgres psql -U davaroutes_user -d davaroutes_db
```

Useful table-count query:

```sql
SELECT 'users' AS table_name, COUNT(*) FROM users
UNION ALL
SELECT 'driver_profiles', COUNT(*) FROM driver_profiles
UNION ALL
SELECT 'vehicles', COUNT(*) FROM vehicles
UNION ALL
SELECT 'vehicle_state_snapshots', COUNT(*) FROM vehicle_state_snapshots
UNION ALL
SELECT 'locations', COUNT(*) FROM locations
UNION ALL
SELECT 'charging_stations', COUNT(*) FROM charging_stations
UNION ALL
SELECT 'fuel_stations', COUNT(*) FROM fuel_stations
UNION ALL
SELECT 'service_centers', COUNT(*) FROM service_centers
UNION ALL
SELECT 'partners', COUNT(*) FROM partners
UNION ALL
SELECT 'partner_locations', COUNT(*) FROM partner_locations
UNION ALL
SELECT 'partner_offers', COUNT(*) FROM partner_offers
UNION ALL
SELECT 'loyalty_accounts', COUNT(*) FROM loyalty_accounts;
```

## Backend Development

From the repository root:

```bash
python -m venv venv
source venv/bin/activate
pip install -r backend/requirements.txt
```

Start PostGIS with Docker:

```bash
docker compose up postgres
```

Then run the API from `backend/`:

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Android App

Open the `android/` directory in Android Studio.

The app currently points Retrofit at:

- `http://10.0.2.2:8000/` on Android emulators
- `http://127.0.0.1:8000/` otherwise

If you test on a physical device, update `android/app/src/main/java/com/example/davaroutes/network/RetrofitClient.kt` to use your machine's LAN IP address, and make sure the backend is reachable from the device.

Build from the command line:

```bash
cd android
./gradlew assembleDebug
```

## Testing

### Backend Tests

```bash
cd backend
pytest
```

### Android Unit Tests

```bash
cd android
./gradlew test
```

### Android Instrumented Tests

These require an emulator or physical device.

```bash
cd android
./gradlew connectedAndroidTest
```

## API Areas

The FastAPI app registers routers for:

- `auth`: registration, login, and Google OAuth
- `users`: user setup/bootstrap flows
- `driver_profiles`: driver preferences and profile data
- `vehicles`: vehicle profile management
- `vehicle_state`: battery, fuel, mileage, and related state snapshots
- `trips`: trip creation and history
- `routes`: Google route preview/computation
- `places`: Google Places search/autocomplete flows
- `recommendations`: contextual stop and partner recommendations

Use `http://localhost:8000/docs` for the exact request and response schemas.

## Troubleshooting

- `JWT_SECRET_KEY` error on backend startup: add `JWT_SECRET_KEY` to the root `.env`.
- Google route/place errors: confirm `GOOGLE_MAPS_API_KEY` is present and the required Google APIs are enabled.
- Google login fails on Android: verify the web OAuth client ID, Android package name, and SHA-1 fingerprint in Google Cloud.
- Android emulator cannot reach backend: use `10.0.2.2:8000`, which is already configured for likely emulator builds.
- Physical device cannot reach backend: use your computer's LAN IP in `RetrofitClient.kt` and allow local network/firewall access.
- Stale local data: run `docker compose down -v`, then rebuild and reseed.

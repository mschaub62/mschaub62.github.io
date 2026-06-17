"""Create and seed the MongoDB database used by the enhanced CS-340 dashboard.

This helper is meant for local testing. It creates the aac database, animals
collection, a least-privilege aacuser account, dashboard indexes, and a small
sample dataset that matches each rescue profile filter.

Run from the enhanced folder:
    py setup_mongo_database.py

Password management notes:
    - The script does not store a hardcoded default database password.
    - If MONGO_PASSWORD is not already set, the script prompts for one.
    - If you press Enter at the prompt, a long random password is generated.
    - A local .env file is written for convenience and is blocked by .gitignore.

Optional environment variables:
    MONGO_ADMIN_USER, MONGO_ADMIN_PASSWORD, MONGO_HOST, MONGO_PORT
    MONGO_USER, MONGO_PASSWORD, MONGO_DATABASE, MONGO_COLLECTION, MONGO_AUTH_SOURCE
"""

from __future__ import annotations

import getpass
import os
import secrets
from datetime import datetime
from typing import Any, Dict, List
from urllib.parse import quote_plus

try:
    from dotenv import load_dotenv
except ImportError:
    load_dotenv = None

if load_dotenv:
    load_dotenv()

from pymongo import ASCENDING, MongoClient
from pymongo.errors import OperationFailure, PyMongoError

HOST = os.getenv("MONGO_HOST", "localhost")
PORT = int(os.getenv("MONGO_PORT", "27017"))
DB_NAME = os.getenv("MONGO_DATABASE", "aac")
COLLECTION_NAME = os.getenv("MONGO_COLLECTION", "animals")
APP_USER = os.getenv("MONGO_USER", "aacuser")
APP_PASSWORD = os.getenv("MONGO_PASSWORD")
AUTH_SOURCE = os.getenv("MONGO_AUTH_SOURCE", DB_NAME)
ADMIN_USER = os.getenv("MONGO_ADMIN_USER")
ADMIN_PASSWORD = os.getenv("MONGO_ADMIN_PASSWORD")

SAMPLE_ANIMALS: List[Dict[str, Any]] = [
    {
        "rec_num": 1,
        "animal_id": "A100001",
        "animal_type": "Dog",
        "name": "River",
        "breed": "Labrador Retriever Mix",
        "color": "Black/White",
        "sex_upon_outcome": "Intact Female",
        "age_upon_outcome": "1 year",
        "age_upon_outcome_in_weeks": 52.0,
        "date_of_birth": "2024-05-01",
        "datetime": datetime(2025, 5, 1, 10, 0),
        "monthyear": "2025-05",
        "outcome_type": "Transfer",
        "location_lat": 30.2672,
        "location_long": -97.7431,
    },
    {
        "rec_num": 2,
        "animal_id": "A100002",
        "animal_type": "Dog",
        "name": "Bay",
        "breed": "Chesa Bay Retr Mix",
        "color": "Brown",
        "sex_upon_outcome": "Intact Female",
        "age_upon_outcome": "2 years",
        "age_upon_outcome_in_weeks": 104.0,
        "date_of_birth": "2023-02-12",
        "datetime": datetime(2025, 6, 3, 13, 20),
        "monthyear": "2025-06",
        "outcome_type": "Transfer",
        "location_lat": 30.2952,
        "location_long": -97.7340,
    },
    {
        "rec_num": 3,
        "animal_id": "A100003",
        "animal_type": "Dog",
        "name": "Summit",
        "breed": "German Shepherd Mix",
        "color": "Tan/Black",
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome": "1 year",
        "age_upon_outcome_in_weeks": 70.0,
        "date_of_birth": "2024-01-15",
        "datetime": datetime(2025, 7, 10, 9, 45),
        "monthyear": "2025-07",
        "outcome_type": "Transfer",
        "location_lat": 30.2500,
        "location_long": -97.7600,
    },
    {
        "rec_num": 4,
        "animal_id": "A100004",
        "animal_type": "Dog",
        "name": "Kodiak",
        "breed": "Alaskan Malamute Mix",
        "color": "Gray/White",
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome": "2 years",
        "age_upon_outcome_in_weeks": 110.0,
        "date_of_birth": "2023-03-01",
        "datetime": datetime(2025, 8, 4, 15, 5),
        "monthyear": "2025-08",
        "outcome_type": "Adoption",
        "location_lat": 30.3100,
        "location_long": -97.7000,
    },
    {
        "rec_num": 5,
        "animal_id": "A100005",
        "animal_type": "Dog",
        "name": "Scout",
        "breed": "Golden Retriever Mix",
        "color": "Gold",
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome": "3 years",
        "age_upon_outcome_in_weeks": 156.0,
        "date_of_birth": "2022-04-21",
        "datetime": datetime(2025, 9, 2, 11, 30),
        "monthyear": "2025-09",
        "outcome_type": "Transfer",
        "location_lat": 30.2800,
        "location_long": -97.7200,
    },
    {
        "rec_num": 6,
        "animal_id": "A100006",
        "animal_type": "Dog",
        "name": "Tracker",
        "breed": "Bloodhound Mix",
        "color": "Red",
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome": "4 years",
        "age_upon_outcome_in_weeks": 208.0,
        "date_of_birth": "2021-01-05",
        "datetime": datetime(2025, 10, 12, 12, 15),
        "monthyear": "2025-10",
        "outcome_type": "Transfer",
        "location_lat": 30.2300,
        "location_long": -97.8000,
    },
    {
        "rec_num": 7,
        "animal_id": "A100007",
        "animal_type": "Dog",
        "name": "Moose",
        "breed": "Newfoundland Mix",
        "color": "Black",
        "sex_upon_outcome": "Intact Female",
        "age_upon_outcome": "1 year",
        "age_upon_outcome_in_weeks": 60.0,
        "date_of_birth": "2024-02-18",
        "datetime": datetime(2025, 11, 1, 10, 10),
        "monthyear": "2025-11",
        "outcome_type": "Transfer",
        "location_lat": 30.2600,
        "location_long": -97.7100,
    },
    {
        "rec_num": 8,
        "animal_id": "A100008",
        "animal_type": "Dog",
        "name": "Ranger",
        "breed": "Rottweiler Mix",
        "color": "Black/Brown",
        "sex_upon_outcome": "Intact Male",
        "age_upon_outcome": "2 years",
        "age_upon_outcome_in_weeks": 120.0,
        "date_of_birth": "2023-06-01",
        "datetime": datetime(2025, 12, 5, 14, 25),
        "monthyear": "2025-12",
        "outcome_type": "Adoption",
        "location_lat": 30.2900,
        "location_long": -97.7800,
    },
    {
        "rec_num": 9,
        "animal_id": "A100009",
        "animal_type": "Cat",
        "name": "Mittens",
        "breed": "Domestic Shorthair Mix",
        "color": "Gray",
        "sex_upon_outcome": "Spayed Female",
        "age_upon_outcome": "2 years",
        "age_upon_outcome_in_weeks": 104.0,
        "date_of_birth": "2023-07-19",
        "datetime": datetime(2025, 12, 10, 16, 0),
        "monthyear": "2025-12",
        "outcome_type": "Adoption",
        "location_lat": 30.3000,
        "location_long": -97.7500,
    },
    {
        "rec_num": 10,
        "animal_id": "A100010",
        "animal_type": "Dog",
        "name": "Piper",
        "breed": "Beagle Mix",
        "color": "Tricolor",
        "sex_upon_outcome": "Spayed Female",
        "age_upon_outcome": "5 years",
        "age_upon_outcome_in_weeks": 260.0,
        "date_of_birth": "2020-05-14",
        "datetime": datetime(2025, 12, 18, 8, 45),
        "monthyear": "2025-12",
        "outcome_type": "Return to Owner",
        "location_lat": 30.2700,
        "location_long": -97.7600,
    },
]


def get_or_create_app_password() -> str:
    """Return a database password without relying on a hardcoded default."""
    existing_password = os.getenv("MONGO_PASSWORD")
    if existing_password:
        return existing_password

    entered_password = getpass.getpass(
        "Enter MongoDB password for the dashboard user, or press Enter to generate one: "
    )
    if entered_password:
        return entered_password

    generated_password = secrets.token_urlsafe(24)
    print("Generated a long random password for the dashboard MongoDB user.")
    return generated_password


def write_local_env_file(password: str) -> None:
    """Write local environment settings to .env for easier dashboard startup."""
    env_file = ".env"
    contents = (
        "# Local dashboard settings. Do not commit this file to GitHub.\n"
        f"MONGO_USER={APP_USER}\n"
        f"MONGO_PASSWORD={password}\n"
        f"MONGO_HOST={HOST}\n"
        f"MONGO_PORT={PORT}\n"
        f"MONGO_DATABASE={DB_NAME}\n"
        f"MONGO_COLLECTION={COLLECTION_NAME}\n"
        f"MONGO_AUTH_SOURCE={AUTH_SOURCE}\n"
    )
    with open(env_file, "w", encoding="utf-8") as file:
        file.write(contents)
    print("Wrote local MongoDB settings to .env. Keep this file private.")


def admin_client() -> MongoClient:
    """Return an admin-capable client, using admin credentials when provided."""
    if ADMIN_USER and ADMIN_PASSWORD:
        user = quote_plus(ADMIN_USER)
        password = quote_plus(ADMIN_PASSWORD)
        return MongoClient(f"mongodb://{user}:{password}@{HOST}:{PORT}/admin")
    return MongoClient(HOST, PORT)


def create_app_user(client: MongoClient, password: str) -> None:
    """Create or update the dashboard user with the supplied password."""
    database = client[AUTH_SOURCE]
    try:
        users = database.command("usersInfo", APP_USER).get("users", [])
        if users:
            database.command("updateUser", APP_USER, pwd=password, roles=[{"role": "readWrite", "db": DB_NAME}])
            print(f"Updated MongoDB user '{APP_USER}' in '{AUTH_SOURCE}' with the current password.")
            return
        database.command(
            "createUser",
            APP_USER,
            pwd=password,
            roles=[{"role": "readWrite", "db": DB_NAME}],
        )
        print(f"Created MongoDB user '{APP_USER}' in '{AUTH_SOURCE}'.")
    except OperationFailure as exc:
        print("Could not create/check the MongoDB user automatically.")
        print("Use mongosh with an admin account and run the createUser command from the README.")
        print(f"MongoDB message: {exc}")


def seed_animals(client: MongoClient) -> None:
    """Create collection, indexes, and sample records."""
    collection = client[DB_NAME][COLLECTION_NAME]
    collection.delete_many({"animal_id": {"$in": [animal["animal_id"] for animal in SAMPLE_ANIMALS]}})
    result = collection.insert_many(SAMPLE_ANIMALS)
    print(f"Inserted {len(result.inserted_ids)} sample animal records into {DB_NAME}.{COLLECTION_NAME}.")

    collection.create_index([("animal_type", ASCENDING)], name="idx_animal_type")
    collection.create_index([("breed", ASCENDING)], name="idx_breed")
    collection.create_index([("sex_upon_outcome", ASCENDING)], name="idx_sex")
    collection.create_index([("age_upon_outcome_in_weeks", ASCENDING)], name="idx_age_weeks")
    collection.create_index([("location_lat", ASCENDING), ("location_long", ASCENDING)], name="idx_location")
    collection.create_index([("animal_id", ASCENDING)], name="idx_animal_id")
    print("Created dashboard indexes.")


def main() -> None:
    try:
        app_password = get_or_create_app_password()
        client = admin_client()
        client.admin.command("ping")
        create_app_user(client, app_password)
        seed_animals(client)
        write_local_env_file(app_password)
        print("\nDatabase setup is complete.")
        print("The dashboard can now read MongoDB settings from the local .env file.")
        print("Do not upload .env or paste the password into GitHub.")
    except PyMongoError as exc:
        print(f"MongoDB setup failed: {exc}")
        raise SystemExit(1) from exc


if __name__ == "__main__":
    main()

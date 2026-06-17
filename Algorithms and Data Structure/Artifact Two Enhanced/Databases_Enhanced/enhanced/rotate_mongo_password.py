"""Rotate the MongoDB dashboard user's password and update the local .env file.

Run from the enhanced folder:
    py rotate_mongo_password.py

The script uses MONGO_ADMIN_USER/MONGO_ADMIN_PASSWORD when provided. Otherwise,
it attempts to connect to the local MongoDB instance without admin credentials,
which works on a default local development install without access control enabled.
"""

from __future__ import annotations

import getpass
import os
import secrets
from urllib.parse import quote_plus

try:
    from dotenv import load_dotenv
except ImportError:
    load_dotenv = None

if load_dotenv:
    load_dotenv()

from pymongo import MongoClient
from pymongo.errors import PyMongoError

HOST = os.getenv("MONGO_HOST", "localhost")
PORT = int(os.getenv("MONGO_PORT", "27017"))
DB_NAME = os.getenv("MONGO_DATABASE", "aac")
AUTH_SOURCE = os.getenv("MONGO_AUTH_SOURCE", DB_NAME)
APP_USER = os.getenv("MONGO_USER", "aacuser")
COLLECTION_NAME = os.getenv("MONGO_COLLECTION", "animals")
ADMIN_USER = os.getenv("MONGO_ADMIN_USER")
ADMIN_PASSWORD = os.getenv("MONGO_ADMIN_PASSWORD")


def admin_client() -> MongoClient:
    if ADMIN_USER and ADMIN_PASSWORD:
        user = quote_plus(ADMIN_USER)
        password = quote_plus(ADMIN_PASSWORD)
        return MongoClient(f"mongodb://{user}:{password}@{HOST}:{PORT}/admin")
    return MongoClient(HOST, PORT)


def write_env(password: str) -> None:
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
    with open(".env", "w", encoding="utf-8") as file:
        file.write(contents)


def main() -> None:
    entered = getpass.getpass(
        "Enter a new MongoDB dashboard password, or press Enter to generate one: "
    )
    new_password = entered or secrets.token_urlsafe(24)

    try:
        client = admin_client()
        client.admin.command("ping")
        client[AUTH_SOURCE].command(
            "updateUser",
            APP_USER,
            pwd=new_password,
            roles=[{"role": "readWrite", "db": DB_NAME}],
        )
        write_env(new_password)
        print(f"Rotated password for MongoDB user '{APP_USER}' and updated .env.")
        print("Keep .env private and do not upload it to GitHub.")
    except PyMongoError as exc:
        print(f"Password rotation failed: {exc}")
        raise SystemExit(1) from exc


if __name__ == "__main__":
    main()

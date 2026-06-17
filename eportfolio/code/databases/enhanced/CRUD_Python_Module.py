"""Enhanced MongoDB CRUD module for the CS-340 Grazioso Salvare dashboard.

This version keeps the CRUD behavior from the original project but adds database
practices that better match a production dashboard: environment-based
configuration, connection validation, indexes, projection support, paging, safe
updates, and server-side aggregation for the breed chart.
"""

from __future__ import annotations

import os
from typing import Any, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple
from urllib.parse import quote_plus

try:
    from dotenv import load_dotenv
except ImportError:  # python-dotenv is optional, but recommended for local setup.
    load_dotenv = None

if load_dotenv:
    load_dotenv()

from pymongo import ASCENDING, MongoClient
from pymongo.collection import Collection
from pymongo.errors import PyMongoError


DEFAULT_PROJECTION: Dict[str, int] = {
    "_id": 0,
    "rec_num": 1,
    "age_upon_outcome": 1,
    "animal_id": 1,
    "animal_type": 1,
    "breed": 1,
    "color": 1,
    "date_of_birth": 1,
    "datetime": 1,
    "monthyear": 1,
    "name": 1,
    "outcome_type": 1,
    "sex_upon_outcome": 1,
    "location_lat": 1,
    "location_long": 1,
    "age_upon_outcome_in_weeks": 1,
}


class AnimalShelter:
    """CRUD operations for the Austin Animal Center MongoDB collection."""

    def __init__(
        self,
        username: Optional[str] = None,
        password: Optional[str] = None,
        host: Optional[str] = None,
        port: Optional[int] = None,
        database: Optional[str] = None,
        collection: Optional[str] = None,
        auth_source: Optional[str] = None,
        server_timeout_ms: int = 5000,
        create_indexes: bool = True,
    ) -> None:
        """Connect to MongoDB using environment variables or supplied arguments.

        Expected environment variables:
            MONGO_USER, MONGO_PASSWORD, MONGO_HOST, MONGO_PORT,
            MONGO_DATABASE, MONGO_COLLECTION, MONGO_AUTH_SOURCE
        """

        self.username = username or os.getenv("MONGO_USER")
        self.password = password or os.getenv("MONGO_PASSWORD")
        self.host = host or os.getenv("MONGO_HOST", "localhost")
        self.port = int(port or os.getenv("MONGO_PORT", "27017"))
        self.database_name = database or os.getenv("MONGO_DATABASE", "aac")
        self.collection_name = collection or os.getenv("MONGO_COLLECTION", "animals")
        self.auth_source = auth_source or os.getenv("MONGO_AUTH_SOURCE", self.database_name)

        if not self.username or not self.password:
            raise ValueError(
                "MongoDB credentials are not set. Define MONGO_USER and "
                "MONGO_PASSWORD in your environment before running the dashboard."
            )

        user = quote_plus(self.username)
        pwd = quote_plus(self.password)
        uri = f"mongodb://{user}:{pwd}@{self.host}:{self.port}/{self.database_name}?authSource={self.auth_source}"

        try:
            self.client = MongoClient(uri, serverSelectionTimeoutMS=server_timeout_ms)
            # Ping makes connection problems fail early instead of during a callback.
            self.client.admin.command("ping")
            self.database = self.client[self.database_name]
            self.collection: Collection = self.database[self.collection_name]
            if create_indexes:
                self.ensure_indexes()
        except PyMongoError as exc:
            raise ConnectionError(f"MongoDB connection failed: {exc}") from exc

    def ensure_indexes(self) -> None:
        """Create indexes used by the dashboard filters and map/table views."""
        index_specs: Sequence[Tuple[Sequence[Tuple[str, int]], str]] = (
            ([('animal_type', ASCENDING)], 'idx_animal_type'),
            ([('breed', ASCENDING)], 'idx_breed'),
            ([('sex_upon_outcome', ASCENDING)], 'idx_sex'),
            ([('age_upon_outcome_in_weeks', ASCENDING)], 'idx_age_weeks'),
            ([('location_lat', ASCENDING), ('location_long', ASCENDING)], 'idx_location'),
            ([('animal_id', ASCENDING)], 'idx_animal_id'),
        )
        for keys, name in index_specs:
            self.collection.create_index(list(keys), name=name, background=True)

    @staticmethod
    def _valid_document(document: Any) -> bool:
        return isinstance(document, Mapping) and bool(document)

    def create(self, data: Mapping[str, Any]) -> bool:
        """Insert one document and return True when the insert is acknowledged."""
        if not self._valid_document(data):
            return False
        try:
            result = self.collection.insert_one(dict(data))
            return bool(result.acknowledged)
        except PyMongoError:
            return False

    def read(
        self,
        query: Optional[Mapping[str, Any]] = None,
        projection: Optional[Mapping[str, int]] = None,
        limit: int = 250,
        skip: int = 0,
        sort: Optional[Iterable[Tuple[str, int]]] = None,
    ) -> List[Dict[str, Any]]:
        """Return matching documents using projection, paging, and optional sorting."""
        if query is None:
            query = {}
        if not isinstance(query, Mapping):
            return []

        clean_projection = dict(projection or DEFAULT_PROJECTION)
        clean_limit = max(1, min(int(limit), 1000))
        clean_skip = max(0, int(skip))

        try:
            cursor = self.collection.find(dict(query), clean_projection).skip(clean_skip).limit(clean_limit)
            if sort:
                cursor = cursor.sort(list(sort))
            return list(cursor)
        except PyMongoError:
            return []

    def update(self, query: Mapping[str, Any], update_data: Mapping[str, Any]) -> int:
        """Update matching documents and return the number of modified records."""
        if not isinstance(query, Mapping) or not query:
            return 0
        if not self._valid_document(update_data):
            return 0

        update_doc = dict(update_data)
        if not any(key.startswith("$") for key in update_doc):
            update_doc = {"$set": update_doc}

        try:
            result = self.collection.update_many(dict(query), update_doc)
            return int(result.modified_count)
        except PyMongoError:
            return 0

    def delete(self, query: Mapping[str, Any]) -> int:
        """Delete matching documents and return the number removed."""
        if not isinstance(query, Mapping) or not query:
            return 0
        try:
            result = self.collection.delete_many(dict(query))
            return int(result.deleted_count)
        except PyMongoError:
            return 0

    def aggregate_breeds(
        self,
        query: Optional[Mapping[str, Any]] = None,
        limit: int = 10,
    ) -> List[Dict[str, Any]]:
        """Return server-side breed counts for the active rescue profile."""
        if query is None:
            query = {}
        if not isinstance(query, Mapping):
            return []

        pipeline = [
            {"$match": dict(query)},
            {"$group": {"_id": "$breed", "count": {"$sum": 1}}},
            {"$sort": {"count": -1, "_id": 1}},
            {"$limit": max(1, min(int(limit), 25))},
            {"$project": {"_id": 0, "breed": "$_id", "count": 1}},
        ]
        try:
            return list(self.collection.aggregate(pipeline))
        except PyMongoError:
            return []

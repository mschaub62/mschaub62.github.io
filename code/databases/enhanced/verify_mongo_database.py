"""Quick verification script for the local CS-340 MongoDB database."""

from CRUD_Python_Module import AnimalShelter

if __name__ == "__main__":
    db = AnimalShelter()
    total = db.collection.count_documents({})
    print(f"Connected successfully. Documents in aac.animals: {total}")
    print("Sample record:")
    print(db.collection.find_one({}, {"_id": 0}))
    print("Indexes:")
    for index in db.collection.list_indexes():
        print(index.get("name"), index.get("key"))

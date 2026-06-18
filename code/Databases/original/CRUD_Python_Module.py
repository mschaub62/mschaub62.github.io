from pymongo import MongoClient 
from pymongo.errors import PyMongoError



class AnimalShelter(object): 
    """ CRUD operations for Animal collection in MongoDB """ 

    def __init__(self, 
        # Initializing the MongoClient. This helps to access the MongoDB 
        # databases and collections. This is hard-wired to use the aac 
        # database, the animals collection, and the aac user. 
        # 
        # You must edit the password below for your environment. 
        # 
        # Connection Variables 
        # 
        username = 'aacuser', 
        password = 'funDB62', 
        host = 'localhost', 
        port = 27017, 
        database = 'aac', 
        collection = 'animals', 
                ):
        # 
        # Initialize Connection 
        # 
        # 
        # Initialize Connection 
        # 
        
        try: 
            self.client = MongoClient('mongodb://%s:%s@%s:%d' %(username,password,host,port)) 
            self.database = self.client['%s' % (database)] 
            self.collection = self.database['%s' % (collection)]
            
        except PyMongoError as exc:
            raise Exception(f"MongoDB connection failed: {exc}")
            
    # Create a method to return the next available record number for use in the create method   
    # Complete this create method to implement the C in CRUD. 
    def create(self, data):
       
        #Insert a document into the collection.
        #Return True if insert succeeds, else False.
       
        if not isinstance(data, dict) or not data:
            return False

        try:
            result = self.collection.insert_one(data)
            return bool(result.acknowledged)
        except PyMongoError:
            return False
   
    # Create method to implement the R in CRUD.
    def read(self, query):
   
        #Query for documents using MongoDB find().
        #Return a list of documents if successful, else an empty list.
     
        if not isinstance(query, dict) or query is None:
            return []

        try:
            cursor = self.collection.find(query)
            return list(cursor)
        except PyMongoError:
            return []
    # Create method to implement the U in CRUD.
    def update(self, query, update_data):
     
        # Update matching document(s).
        # Return the number of documents modified.
      
        if not isinstance(query, dict) or query is None:
            return 0
        if not isinstance(update_data, dict) or not update_data:
            return 0

        update_doc = update_data if any(k.startswith("$") for k in update_data.keys()) else {"$set": update_data}

        try:
            result = self.collection.update_many(query, update_doc)
            return int(result.modified_count)
        except PyMongoError:
            return 0
    # Create method to implement the D in CRUD.
    def delete(self, query):
        
        # Delete matching document(s).
        # Return the number of documents deleted.
        
        if not isinstance(query, dict) or query is None:
            return 0

        try:
            result = self.collection.delete_many(query)
            return int(result.deleted_count)
        except PyMongoError:
            return 0
    
        
    
    
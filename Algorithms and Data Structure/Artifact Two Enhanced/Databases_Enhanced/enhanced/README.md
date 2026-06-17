# CS-340 Grazioso Salvare Dashboard - Database Enhancement

This folder contains the enhanced database version of the CS-340 Grazioso Salvare dashboard. The original project used MongoDB, PyMongo, Dash, Plotly, and Dash Leaflet to filter Austin Animal Center records for rescue-dog profiles. The enhanced version keeps that purpose but strengthens the database layer, dashboard design, and password handling.

## Main enhancements

- Removed hardcoded MongoDB credentials from the source code.
- Added environment-based database configuration using `MONGO_USER`, `MONGO_PASSWORD`, `MONGO_HOST`, `MONGO_PORT`, `MONGO_DATABASE`, `MONGO_COLLECTION`, and `MONGO_AUTH_SOURCE`.
- Added optional `.env` loading for local development with `python-dotenv`.
- Added `.gitignore` rules so `.env` and local CSV files are not committed to GitHub.
- Changed `setup_mongo_database.py` so it prompts for a password or generates a long random password instead of using a fixed default password.
- Added `rotate_mongo_password.py` so the MongoDB dashboard password can be changed later.
- Added a connection ping so MongoDB issues fail early with a readable error.
- Added indexes for frequently queried fields: animal type, breed, sex, age, location, and animal ID.
- Added projection and paging to reduce the amount of data sent to the dashboard.
- Validated filter choices by using pre-approved rescue-profile queries instead of raw user-entered MongoDB queries.
- Moved the breed distribution chart to a MongoDB aggregation pipeline.
- Removed editable table behavior to better protect data integrity.
- Updated the map callback to use column names instead of fixed column numbers.
- Added empty-state handling for the table, chart, and map.

## Install packages

From the `enhanced` folder:

```powershell
py -m pip install -r requirements.txt
```

## Better password management

The dashboard now avoids hardcoding the MongoDB password in Python files or PowerShell scripts. For local development, run the setup script and let it create a private `.env` file.

```powershell
py setup_mongo_database.py
```

When prompted, either type your own strong password or press Enter to generate one. The script will create or update the MongoDB user and write the local settings to `.env`.

Do not upload `.env` to GitHub. The `.gitignore` file blocks it, but you should still avoid copying the password into screenshots, documentation, or commits.

To rotate the local MongoDB password later, run:

```powershell
py rotate_mongo_password.py
```

## Run the dashboard

After `setup_mongo_database.py` creates the local `.env` file, start the dashboard with:

```powershell
.\start_dashboard.ps1
```

Or run the Python file directly:

```powershell
py ProjectTwoDashboard_Enhanced.py
```

The dashboard starts on port `8051` by default:

```text
http://127.0.0.1:8051
```

## Create a local test database

If you do not already have the original Austin Animal Center dataset loaded, `setup_mongo_database.py` creates a small local test database that lets the dashboard run and proves the filters, table, chart, and map work.

The helper creates the `aac` database, the `animals` collection, a least-privilege `aacuser` account, dashboard indexes, and sample animal records for the Reset, Water Rescue, Mountain/Wilderness, and Disaster filters.

If automatic user creation fails because MongoDB requires an admin login, set admin credentials before running the setup script:

```powershell
$env:MONGO_ADMIN_USER="your-admin-user"
$env:MONGO_ADMIN_PASSWORD="your-admin-password"
py setup_mongo_database.py
```

## Optional full CSV import

If you have the original `aac_shelter_outcomes.csv`, you can import the full dataset instead of using only the sample records:

```powershell
mongoimport --username aacuser --password "your-password-here" --authenticationDatabase aac --db aac --collection animals --type csv --file ".\aac_shelter_outcomes.csv" --headerline --drop
```

After importing the full CSV, run the dashboard. The enhanced CRUD module will create the indexes when it connects.

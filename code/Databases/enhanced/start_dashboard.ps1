# Starts the enhanced CS-340 dashboard without hardcoding the MongoDB password.
# Run setup_mongo_database.py first. That script creates a local .env file.

if (-not (Test-Path ".\.env")) {
    Write-Host "No .env file found. Run this first:" -ForegroundColor Yellow
    Write-Host "py setup_mongo_database.py" -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting CS-340 dashboard on http://127.0.0.1:8051"
Write-Host "MongoDB credentials will be loaded from the local .env file. Do not commit .env to GitHub."
py ProjectTwoDashboard_Enhanced.py

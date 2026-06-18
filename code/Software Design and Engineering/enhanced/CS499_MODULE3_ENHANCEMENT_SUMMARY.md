# CS 499 Module 3 Software Design and Engineering Enhancement

Artifact: CS 360 Weight Tracking Application  
Student: Matthew Schaub

## Enhancement Summary
This version enhances the original Android/Java weight tracking application for the CS 499 software design and engineering milestone. The focus was to improve architecture, input validation, authentication security, SMS permission behavior, and maintainability while preserving the original app purpose.

## Key Changes
- Added `WeightRepository.java` to separate Activity UI logic from direct SQLite access.
- Replaced plain-text password storage with salted PBKDF2 hashing in `PasswordUtils.java`.
- Updated `DatabaseHelper.java` schema to store `password_hash` and `password_salt` instead of plain passwords.
- Added `WeightValidator.java` to centralize username, password, weight, and phone-number validation.
- Fixed goal-based SMS logic so it now retrieves the active user's goal weight instead of calling a method that always returned zero.
- Moved SMS settings into the SQLite database per user through `SmsSettings.java` and repository methods.
- Added delete confirmation before removing weight records from the dashboard.
- Removed static/sample dashboard rows so the grid reflects real database entries only.
- Added local unit tests for password hashing and validation utilities.

## Files Most Directly Enhanced
- `app/src/main/java/com/example/weighttrackingapplication/DatabaseHelper.java`
- `app/src/main/java/com/example/weighttrackingapplication/WeightRepository.java`
- `app/src/main/java/com/example/weighttrackingapplication/PasswordUtils.java`
- `app/src/main/java/com/example/weighttrackingapplication/WeightValidator.java`
- `app/src/main/java/com/example/weighttrackingapplication/SmsSettings.java`
- `app/src/main/java/com/example/weighttrackingapplication/LoginActivity.java`
- `app/src/main/java/com/example/weighttrackingapplication/AddWeightActivity.java`
- `app/src/main/java/com/example/weighttrackingapplication/DashboardActivity.java`
- `app/src/main/java/com/example/weighttrackingapplication/GoalWeightActivity.java`
- `app/src/main/java/com/example/weighttrackingapplication/SmsSettingsActivity.java`
- `app/src/test/java/com/example/weighttrackingapplication/PasswordUtilsTest.java`
- `app/src/test/java/com/example/weighttrackingapplication/WeightValidatorTest.java`

## Validation Note
The pure Java utility classes were syntax-checked with `javac`. A full Gradle build could not be executed in this environment because the Gradle wrapper attempted to download the Gradle distribution and the container has no external network access.

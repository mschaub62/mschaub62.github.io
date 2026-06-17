package com.example.weighttrackingapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper manages the local SQLite database used by the application.
 *
 * Enhancement highlights:
 * - Passwords are stored as salted PBKDF2 hashes instead of plain text.
 * - SMS settings are stored per user, not as a single global setting.
 * - Queries remain parameterized to protect user data.
 * - Indexes support faster lookup of user-specific weight history and goals.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_WEIGHTS = "weights";
    public static final String TABLE_GOALS = "goals";
    public static final String TABLE_SETTINGS = "settings";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "password_salt TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_WEIGHTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "entry_date TEXT NOT NULL, "
                + "weight REAL NOT NULL CHECK(weight >= 20 AND weight <= 1000), "
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + TABLE_GOALS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL UNIQUE, "
                + "goal_weight REAL NOT NULL CHECK(goal_weight >= 20 AND goal_weight <= 1000), "
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL UNIQUE, "
                + "sms_phone TEXT NOT NULL DEFAULT '', "
                + "sms_enabled INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id) ON DELETE CASCADE)");

        db.execSQL("CREATE INDEX idx_weights_user_date ON " + TABLE_WEIGHTS + "(user_id, entry_date DESC)");
        db.execSQL("CREATE INDEX idx_goals_user ON " + TABLE_GOALS + "(user_id)");
        db.execSQL("CREATE INDEX idx_settings_user ON " + TABLE_SETTINGS + "(user_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This student project does not need to preserve old sample data during enhancement review.
        // Rebuilding the schema ensures the new security columns and per-user settings table exist.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /**
     * Creates a new user account after hashing the password.
     */
    public boolean createUser(String username, String password) {
        PasswordUtils.PasswordRecord passwordRecord = PasswordUtils.hashPassword(password);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password_hash", passwordRecord.getHash());
        values.put("password_salt", passwordRecord.getSalt());

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    /**
     * Returns the user id when the username exists and the password hash matches.
     */
    public int validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, password_hash, password_salt FROM " + TABLE_USERS + " WHERE username = ?",
                new String[]{username}
        );

        int userId = -1;
        try {
            if (cursor.moveToFirst()) {
                String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
                String storedSalt = cursor.getString(cursor.getColumnIndexOrThrow("password_salt"));
                if (PasswordUtils.verifyPassword(password, storedHash, storedSalt)) {
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                }
            }
        } finally {
            cursor.close();
        }
        return userId;
    }

    /**
     * Inserts a new weight entry for the active user.
     */
    public long addWeight(int userId, String entryDate, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("entry_date", entryDate);
        values.put("weight", weight);
        return db.insert(TABLE_WEIGHTS, null, values);
    }

    /**
     * Updates an existing weight entry that belongs to the active user.
     */
    public int updateWeight(int weightId, int userId, String entryDate, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("entry_date", entryDate);
        values.put("weight", weight);
        return db.update(
                TABLE_WEIGHTS,
                values,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(weightId), String.valueOf(userId)}
        );
    }

    /**
     * Deletes one weight entry owned by the active user.
     */
    public int deleteWeight(int weightId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(
                TABLE_WEIGHTS,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(weightId), String.valueOf(userId)}
        );
    }

    /**
     * Loads all weight entries for the active user.
     */
    public List<WeightEntry> getAllWeightsForUser(int userId) {
        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, entry_date, weight FROM " + TABLE_WEIGHTS
                        + " WHERE user_id = ? ORDER BY entry_date DESC, id DESC",
                new String[]{String.valueOf(userId)}
        );

        try {
            while (cursor.moveToNext()) {
                entries.add(new WeightEntry(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("entry_date")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("weight"))
                ));
            }
        } finally {
            cursor.close();
        }
        return entries;
    }

    /**
     * Loads one weight entry for editing and verifies that it belongs to the active user.
     */
    public WeightEntry getWeightById(int weightId, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, entry_date, weight FROM " + TABLE_WEIGHTS
                        + " WHERE id = ? AND user_id = ?",
                new String[]{String.valueOf(weightId), String.valueOf(userId)}
        );

        WeightEntry entry = null;
        try {
            if (cursor.moveToFirst()) {
                entry = new WeightEntry(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("entry_date")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("weight"))
                );
            }
        } finally {
            cursor.close();
        }
        return entry;
    }

    /**
     * Saves or updates the goal weight for the active user.
     */
    public long saveGoalWeight(int userId, double goalWeight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("goal_weight", goalWeight);

        long existingCount = DatabaseUtilsCompat.queryLong(
                db,
                "SELECT COUNT(*) FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (existingCount > 0) {
            return db.update(
                    TABLE_GOALS,
                    values,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)}
            );
        }
        return db.insert(TABLE_GOALS, null, values);
    }

    /**
     * Returns the saved goal weight for the active user or null when no goal exists.
     */
    public Double getGoalWeight(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT goal_weight FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        Double goalWeight = null;
        try {
            if (cursor.moveToFirst()) {
                goalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow("goal_weight"));
            }
        } finally {
            cursor.close();
        }
        return goalWeight;
    }

    /**
     * Saves SMS settings for the active user only.
     */
    public long saveSmsSettings(int userId, String phoneNumber, boolean smsEnabled) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("sms_phone", phoneNumber == null ? "" : phoneNumber);
        values.put("sms_enabled", smsEnabled ? 1 : 0);

        long existingCount = DatabaseUtilsCompat.queryLong(
                db,
                "SELECT COUNT(*) FROM " + TABLE_SETTINGS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (existingCount > 0) {
            return db.update(
                    TABLE_SETTINGS,
                    values,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)}
            );
        }
        return db.insert(TABLE_SETTINGS, null, values);
    }

    /**
     * Returns SMS settings for the active user.
     */
    public SmsSettings getSmsSettings(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT sms_phone, sms_enabled FROM " + TABLE_SETTINGS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        SmsSettings settings = new SmsSettings("", false);
        try {
            if (cursor.moveToFirst()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("sms_phone"));
                boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow("sms_enabled")) == 1;
                settings = new SmsSettings(phoneNumber, enabled);
            }
        } finally {
            cursor.close();
        }
        return settings;
    }

    /**
     * Small local helper avoids pulling in additional dependencies for scalar queries.
     */
    private static final class DatabaseUtilsCompat {
        private static long queryLong(SQLiteDatabase db, String sql, String[] bindArgs) {
            SQLiteStatement statement = db.compileStatement(sql);
            try {
                if (bindArgs != null) {
                    for (int index = 0; index < bindArgs.length; index++) {
                        statement.bindString(index + 1, bindArgs[index]);
                    }
                }
                return statement.simpleQueryForLong();
            } finally {
                statement.close();
            }
        }
    }
}

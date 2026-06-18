package com.example.weighttrackingapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * AppDatabaseHelper manages the SQLite database used by the application.
 * It stores users, weight entries, goal weights, and SMS settings.
 */
class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_WEIGHTS = "weights";
    public static final String TABLE_GOALS = "goals";
    public static final String TABLE_SETTINGS = "settings";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_WEIGHTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "entry_date TEXT NOT NULL, "
                + "weight REAL NOT NULL, "
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_GOALS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL UNIQUE, "
                + "goal_weight REAL NOT NULL, "
                + "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " ("
                + "id INTEGER PRIMARY KEY CHECK(id = 1), "
                + "sms_phone TEXT, "
                + "sms_enabled INTEGER NOT NULL DEFAULT 0)");

        ContentValues defaults = new ContentValues();
        defaults.put("id", 1);
        defaults.put("sms_phone", "");
        defaults.put("sms_enabled", 0);
        db.insert(TABLE_SETTINGS, null, defaults);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    /**
     * Creates a new user account if the username is not already taken.
     */
    public boolean createUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    /**
     * Returns the user id when the username and password match a record.
     */
    public int validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_USERS + " WHERE username = ? AND password = ?",
                new String[]{username, password}
        );

        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();
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
     * Updates an existing weight entry.
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
     * Deletes a weight entry for the active user.
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

        while (cursor.moveToNext()) {
            entries.add(new WeightEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("entry_date")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("weight"))
            ));
        }
        cursor.close();
        return entries;
    }

    /**
     * Loads a single weight entry for editing.
     */
    public WeightEntry getWeightById(int weightId, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, entry_date, weight FROM " + TABLE_WEIGHTS
                        + " WHERE id = ? AND user_id = ?",
                new String[]{String.valueOf(weightId), String.valueOf(userId)}
        );

        WeightEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = new WeightEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("entry_date")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("weight"))
            );
        }
        cursor.close();
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

        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        long result;
        if (cursor.moveToFirst()) {
            result = db.update(
                    TABLE_GOALS,
                    values,
                    "user_id = ?",
                    new String[]{String.valueOf(userId)}
            );
        } else {
            result = db.insert(TABLE_GOALS, null, values);
        }
        cursor.close();
        return result;
    }

    /**
     * Returns the saved goal weight for the active user.
     */
    public Double getGoalWeight(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT goal_weight FROM " + TABLE_GOALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        Double goalWeight = null;
        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow("goal_weight"));
        }
        cursor.close();
        return goalWeight;
    }

    /**
     * Saves the SMS phone number and whether alerts are enabled.
     */
    public long saveSmsSettings(String phoneNumber, boolean smsEnabled) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sms_phone", phoneNumber);
        values.put("sms_enabled", smsEnabled ? 1 : 0);

        return db.update(TABLE_SETTINGS, values, "id = 1", null);
    }

    /**
     * Returns the saved phone number for SMS alerts.
     */
    public String getSmsPhoneNumber() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT sms_phone FROM " + TABLE_SETTINGS + " WHERE id = 1",
                null
        );

        String phoneNumber = "";
        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("sms_phone"));
        }
        cursor.close();
        return phoneNumber;
    }

    /**
     * Returns true when SMS alerts are enabled in app settings.
     */
    public boolean isSmsEnabled() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT sms_enabled FROM " + TABLE_SETTINGS + " WHERE id = 1",
                null
        );

        boolean enabled = false;
        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(cursor.getColumnIndexOrThrow("sms_enabled")) == 1;
        }
        cursor.close();
        return enabled;
    }

    public double getGoalWeight() {
        return 0;
    }
}

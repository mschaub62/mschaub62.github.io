package com.example.weighttrackingapplication;

import android.content.Context;

import java.util.List;

/**
 * Repository layer that separates Activity UI logic from direct SQLite calls.
 *
 * This is a lightweight architectural enhancement that moves the app closer to
 * the MVVM/repository structure planned for the ePortfolio enhancement.
 */
public class WeightRepository {
    private final DatabaseHelper databaseHelper;

    public WeightRepository(Context context) {
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public boolean createUser(String username, String password) {
        return databaseHelper.createUser(username, password);
    }

    public int validateUser(String username, String password) {
        return databaseHelper.validateUser(username, password);
    }

    public long addWeight(int userId, String entryDate, double weight) {
        return databaseHelper.addWeight(userId, entryDate, weight);
    }

    public int updateWeight(int weightId, int userId, String entryDate, double weight) {
        return databaseHelper.updateWeight(weightId, userId, entryDate, weight);
    }

    public int deleteWeight(int weightId, int userId) {
        return databaseHelper.deleteWeight(weightId, userId);
    }

    public List<WeightEntry> getAllWeightsForUser(int userId) {
        return databaseHelper.getAllWeightsForUser(userId);
    }

    public WeightEntry getWeightById(int weightId, int userId) {
        return databaseHelper.getWeightById(weightId, userId);
    }

    public long saveGoalWeight(int userId, double goalWeight) {
        return databaseHelper.saveGoalWeight(userId, goalWeight);
    }

    public Double getGoalWeight(int userId) {
        return databaseHelper.getGoalWeight(userId);
    }

    public long saveSmsSettings(int userId, String phoneNumber, boolean smsEnabled) {
        return databaseHelper.saveSmsSettings(userId, phoneNumber, smsEnabled);
    }

    public SmsSettings getSmsSettings(int userId) {
        return databaseHelper.getSmsSettings(userId);
    }
}

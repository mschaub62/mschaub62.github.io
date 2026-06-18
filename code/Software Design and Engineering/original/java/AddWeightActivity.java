package com.example.weighttrackingapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * AddWeightActivity lets the user create a new weight entry or update an existing one.
 */
public class AddWeightActivity extends AppCompatActivity {

    private static final String SESSION_PREFS = "weight_tracker_session";
    private static final String KEY_USER_ID = "user_id";

    private EditText etWeight;
    private DatePicker datePickerWeight;
    private TextView tvEntryMode;
    private DatabaseHelper databaseHelper;
    private int userId;
    private int weightId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

        tvEntryMode = findViewById(R.id.tvAddTitle);
        databaseHelper = new DatabaseHelper(this);
        userId = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).getInt(KEY_USER_ID, -1);

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        etWeight = findViewById(R.id.etWeight);
        datePickerWeight = findViewById(R.id.datePickerWeight);
        Button btnSaveWeight = this.findViewById(R.id.btnSaveWeight);

        weightId = getIntent().getIntExtra("weight_id", -1);
        if (weightId != -1) {
            loadExistingEntry();
        }

        btnSaveWeight.setOnClickListener(v -> saveWeightEntry());
    }

    /**
     * Loads an existing weight entry into the form for editing.
     */
    private void loadExistingEntry() {
        WeightEntry entry = databaseHelper.getWeightById(weightId, userId);
        if (entry == null) {
            Toast.makeText(this, "Unable to load the selected entry.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEntryMode.setText(R.string.update_an_existing_weight_entry);
        etWeight.setText(String.format(Locale.US, "%.1f", entry.getWeight()));

        String[] parts = entry.getEntryDate().split("/");
        if (parts.length == 3) {
            int month = Integer.parseInt(parts[0]) - 1;
            int day = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            datePickerWeight.updateDate(year, month, day);
        }
    }

    /**
     * Saves the current weight entry to the SQLite database.
     */
    private void saveWeightEntry() {
        String weightText = etWeight.getText().toString().trim();

        if (weightText.isEmpty()) {
            Toast.makeText(this, "Please enter a weight.", Toast.LENGTH_SHORT).show();
            return;
        }

        double weightValue;
        try {
            weightValue = Double.parseDouble(weightText);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Enter a valid numeric weight.", Toast.LENGTH_SHORT).show();
            return;
        }

        String entryDate = String.format(
                Locale.US,
                "%02d/%02d/%04d",
                datePickerWeight.getMonth() + 1,
                datePickerWeight.getDayOfMonth(),
                datePickerWeight.getYear()
        );

        boolean wasSaved;
        if (weightId == -1) {
            wasSaved = databaseHelper.addWeight(userId, entryDate, weightValue) != -1;
        } else {
            wasSaved = databaseHelper.updateWeight(weightId, userId, entryDate, weightValue) > 0;
        }

        if (wasSaved) {
            checkGoalAndSendSms(weightValue);
            Toast.makeText(this, weightId == -1 ? "Weight entry saved." : "Weight entry updated.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AddWeightActivity.this, DashboardActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Unable to save the weight entry.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends an SMS alert when the user reaches or goes below the goal weight and permission exists.
     */
    private void checkGoalAndSendSms(double enteredWeight) {
        // Get the saved goal weight from your database helper
        double goalWeight = databaseHelper.getGoalWeight();

        // Stop if no valid goal exists
        if (goalWeight <= 0) {
            return;
        }

        // In a weight-loss app, reaching goal usually means current weight is less than or equal to goal
        if (enteredWeight > goalWeight) {
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("WeightTrackingPrefs", MODE_PRIVATE);
        boolean smsAlertsEnabled = sharedPreferences.getBoolean("sms_alerts_enabled", false);
        String phoneNumber = sharedPreferences.getString("sms_phone_number", "").trim();

        // If user denied permission or never enabled SMS, app continues normally without texting
        if (!smsAlertsEnabled || phoneNumber.isEmpty()) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String message = "Congratulations! You reached your goal weight of "
                + goalWeight + " lbs. Current weight: " + enteredWeight + " lbs.";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Goal reached SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this,
                    "SMS could not be sent on this device/emulator, but the app is still working.",
                    Toast.LENGTH_LONG).show();
        }
    }
}

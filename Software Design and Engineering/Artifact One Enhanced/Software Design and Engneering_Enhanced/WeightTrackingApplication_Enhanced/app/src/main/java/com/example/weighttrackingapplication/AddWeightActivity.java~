package com.example.weighttrackingapplication;

import android.Manifest;
import android.content.Intent;
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
    private WeightRepository repository;
    private int userId;
    private int weightId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

        tvEntryMode = findViewById(R.id.tvAddTitle);
        repository = new WeightRepository(this);
        userId = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).getInt(KEY_USER_ID, -1);

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        etWeight = findViewById(R.id.etWeight);
        datePickerWeight = findViewById(R.id.datePickerWeight);
        Button btnSaveWeight = findViewById(R.id.btnSaveWeight);

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
        WeightEntry entry = repository.getWeightById(weightId, userId);
        if (entry == null) {
            Toast.makeText(this, "Unable to load the selected entry.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvEntryMode.setText(R.string.update_an_existing_weight_entry);
        etWeight.setText(String.format(Locale.US, "%.1f", entry.getWeight()));
        updateDatePicker(entry.getEntryDate());
    }

    private void updateDatePicker(String savedDate) {
        String[] isoParts = savedDate.split("-");
        if (isoParts.length == 3) {
            datePickerWeight.updateDate(
                    Integer.parseInt(isoParts[0]),
                    Integer.parseInt(isoParts[1]) - 1,
                    Integer.parseInt(isoParts[2])
            );
            return;
        }

        String[] slashParts = savedDate.split("/");
        if (slashParts.length == 3) {
            datePickerWeight.updateDate(
                    Integer.parseInt(slashParts[2]),
                    Integer.parseInt(slashParts[0]) - 1,
                    Integer.parseInt(slashParts[1])
            );
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

        WeightValidator.ValidationResult validationResult = WeightValidator.validateWeight(weightValue);
        if (!validationResult.isValid()) {
            Toast.makeText(this, validationResult.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        String entryDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                datePickerWeight.getYear(),
                datePickerWeight.getMonth() + 1,
                datePickerWeight.getDayOfMonth()
        );

        boolean wasSaved;
        if (weightId == -1) {
            wasSaved = repository.addWeight(userId, entryDate, weightValue) != -1;
        } else {
            wasSaved = repository.updateWeight(weightId, userId, entryDate, weightValue) > 0;
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
        Double goalWeight = repository.getGoalWeight(userId);
        if (goalWeight == null || goalWeight <= 0) {
            return;
        }

        if (enteredWeight > goalWeight) {
            return;
        }

        SmsSettings settings = repository.getSmsSettings(userId);
        if (!settings.isEnabled() || settings.getPhoneNumber().trim().isEmpty()) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String message = String.format(
                Locale.US,
                "Congratulations! You reached your goal weight of %.1f lbs. Current weight: %.1f lbs.",
                goalWeight,
                enteredWeight
        );

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(settings.getPhoneNumber(), null, message, null, null);
            Toast.makeText(this, "Goal reached SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this,
                    "SMS could not be sent on this device/emulator, but the app is still working.",
                    Toast.LENGTH_LONG).show();
        }
    }
}

package com.example.weighttrackingapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

/**
 * SmsSettingsActivity controls optional goal-notification SMS behavior.
 * The app continues to function when SMS is disabled or permission is denied.
 */
public class SmsSettingsActivity extends AppCompatActivity {

    private static final String SESSION_PREFS = "weight_tracker_session";
    private static final String KEY_USER_ID = "user_id";

    private SwitchCompat switchSmsAlerts;
    private EditText etPhoneNumber;
    private TextView tvPermissionStatus;
    private TextView tvSmsResult;
    private WeightRepository repository;
    private int userId;
    private boolean smsPermissionGranted = false;

    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                smsPermissionGranted = isGranted;
                updatePermissionStatus();

                if (isGranted) {
                    Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    switchSmsAlerts.setChecked(false);
                    Toast.makeText(this,
                            "SMS permission denied. App will still work without SMS.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_settings);

        repository = new WeightRepository(this);
        userId = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).getInt(KEY_USER_ID, -1);
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        switchSmsAlerts = findViewById(R.id.switchSmsAlerts);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        tvSmsResult = findViewById(R.id.tvSmsResult);
        Button btnRequestSmsPermission = findViewById(R.id.btnGrantPermission);
        Button btnSaveSmsSettings = findViewById(R.id.btnSaveSmsSettings);
        Button btnTestSms = findViewById(R.id.btnSendTestMessage);

        checkCurrentSmsPermission();
        updatePermissionStatus();
        loadSavedSettings();

        btnRequestSmsPermission.setOnClickListener(v -> requestSmsPermission());
        btnSaveSmsSettings.setOnClickListener(v -> saveSmsSettings());
        btnTestSms.setOnClickListener(v -> sendTestSms());

        switchSmsAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !smsPermissionGranted) {
                requestSmsPermission();
            }
        });
    }

    private void checkCurrentSmsPermission() {
        smsPermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void updatePermissionStatus() {
        if (tvPermissionStatus != null) {
            tvPermissionStatus.setText(smsPermissionGranted
                    ? "SMS permission status: granted"
                    : "SMS permission status: not granted");
        }
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            smsPermissionGranted = true;
            updatePermissionStatus();
            Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }

    private void loadSavedSettings() {
        SmsSettings settings = repository.getSmsSettings(userId);
        etPhoneNumber.setText(settings.getPhoneNumber());
        switchSmsAlerts.setChecked(settings.isEnabled() && smsPermissionGranted);
    }

    private void saveSmsSettings() {
        boolean alertsEnabled = switchSmsAlerts.isChecked();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (alertsEnabled) {
            WeightValidator.ValidationResult phoneResult = WeightValidator.validatePhoneNumber(phoneNumber);
            if (!phoneResult.isValid()) {
                tvSmsResult.setText(phoneResult.getMessage());
                Toast.makeText(this, phoneResult.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (alertsEnabled && !smsPermissionGranted) {
            switchSmsAlerts.setChecked(false);
            alertsEnabled = false;
            Toast.makeText(this,
                    "SMS permission is required. The app will continue to work without SMS alerts.",
                    Toast.LENGTH_LONG).show();
        }

        long result = repository.saveSmsSettings(userId, phoneNumber, alertsEnabled);
        if (result != -1) {
            tvSmsResult.setText("SMS settings saved.");
            Toast.makeText(this, "SMS settings saved.", Toast.LENGTH_SHORT).show();
        } else {
            tvSmsResult.setText("Unable to save SMS settings.");
            Toast.makeText(this, "Unable to save SMS settings.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendTestSms() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (!switchSmsAlerts.isChecked()) {
            tvSmsResult.setText("Enable SMS alerts first.");
            Toast.makeText(this, "Enable SMS alerts first.", Toast.LENGTH_SHORT).show();
            return;
        }

        WeightValidator.ValidationResult phoneResult = WeightValidator.validatePhoneNumber(phoneNumber);
        if (!phoneResult.isValid()) {
            tvSmsResult.setText(phoneResult.getMessage());
            Toast.makeText(this, phoneResult.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            tvSmsResult.setText("SMS permission is required.");
            Toast.makeText(this, "SMS permission is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    "Test message from Weight Tracking Application.",
                    null,
                    null
            );
            tvSmsResult.setText("Test SMS sent.");
            Toast.makeText(this, "Test SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            tvSmsResult.setText("Unable to send SMS on this device/emulator.");
            Toast.makeText(this,
                    "Unable to send SMS on this device/emulator.",
                    Toast.LENGTH_LONG).show();
        }
    }
}

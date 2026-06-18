package com.example.weighttrackingapplication;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

public class SmsSettingsActivity extends AppCompatActivity {

    private SwitchCompat switchSmsAlerts;
    private EditText etPhoneNumber;
    private Button btnRequestSmsPermission;
    private Button btnSaveSmsSettings;
    private Button btnTestSms;

    private SharedPreferences sharedPreferences;
    private boolean smsPermissionGranted = false;

    private final ActivityResultLauncher<String> smsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                smsPermissionGranted = isGranted;

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

        switchSmsAlerts = findViewById(R.id.switchSmsAlerts);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnRequestSmsPermission = findViewById(R.id.btnGrantPermission);
        btnSaveSmsSettings = findViewById(R.id.btnSaveSmsSettings);
        btnTestSms = findViewById(R.id.btnSendTestMessage);

        sharedPreferences = getSharedPreferences("WeightTrackingPrefs", MODE_PRIVATE);

        checkCurrentSmsPermission();
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

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            smsPermissionGranted = true;
            Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }

    private void loadSavedSettings() {
        boolean alertsEnabled = sharedPreferences.getBoolean("sms_alerts_enabled", false);
        String phoneNumber = sharedPreferences.getString("sms_phone_number", "");

        etPhoneNumber.setText(phoneNumber);
        switchSmsAlerts.setChecked(alertsEnabled && smsPermissionGranted);
    }

    private void saveSmsSettings() {
        boolean alertsEnabled = switchSmsAlerts.isChecked();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (alertsEnabled && phoneNumber.isEmpty()) {
            Toast.makeText(this, "Enter a phone number first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (alertsEnabled && !smsPermissionGranted) {
            switchSmsAlerts.setChecked(false);
            Toast.makeText(this,
                    "SMS permission is required. The app will continue to work without SMS alerts.",
                    Toast.LENGTH_LONG).show();
            alertsEnabled = false;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("sms_alerts_enabled", alertsEnabled);
        editor.putString("sms_phone_number", phoneNumber);
        editor.apply();

        Toast.makeText(this, "SMS settings saved.", Toast.LENGTH_SHORT).show();
    }

    private void sendTestSms() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (!switchSmsAlerts.isChecked()) {
            Toast.makeText(this, "Enable SMS alerts first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Enter a phone number first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
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
            Toast.makeText(this, "Test SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this,
                    "Unable to send SMS on this device/emulator.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
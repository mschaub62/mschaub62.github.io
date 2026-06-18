package com.example.weighttrackingapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity allows a user to log in or create a new account.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String SESSION_PREFS = "weight_tracker_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvLoginStatus;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvLoginStatus = findViewById(R.id.tvLoginStatus);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnCreateAccount.setOnClickListener(v -> createAccount());
    }

    /**
     * Verifies the username and password against the SQLite database.
     */
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tvLoginStatus.setText("Please enter a username and password.");
            return;
        }

        int userId = databaseHelper.validateUser(username, password);
        if (userId != -1) {
            saveSession(userId, username);
            tvLoginStatus.setText("Login successful.");
            openDashboard();
        } else {
            tvLoginStatus.setText("Login failed. Check your username and password.");
        }
    }

    /**
     * Creates a new account and stores the user information in SQLite.
     */
    private void createAccount() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tvLoginStatus.setText("Enter a username and password to create an account.");
            return;
        }

        boolean created = databaseHelper.createUser(username, password);
        if (created) {
            int userId = databaseHelper.validateUser(username, password);
            saveSession(userId, username);
            tvLoginStatus.setText("Account created successfully.");
            openDashboard();
        } else {
            tvLoginStatus.setText("That username already exists. Please choose another one.");
        }
    }

    /**
     * Saves the active user to SharedPreferences so other screens know who is logged in.
     */
    private void saveSession(int userId, String username) {
        SharedPreferences preferences = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    /**
     * Opens the dashboard screen after a successful login.
     */
    private void openDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}

package com.example.weighttrackingapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

/**
 * DashboardActivity displays the goal summary and a grid of recorded weights.
 * The user can add, update, delete, and view weight entries from this screen.
 */
public class DashboardActivity extends AppCompatActivity {

    private static final String SESSION_PREFS = "weight_tracker_session";
    private static final String KEY_USER_ID = "user_id";

    private DatabaseHelper databaseHelper;
    private int userId;
    private TextView tvGoalSummary;
    private TextView tvEmptyState;
    private TableLayout tableWeights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        databaseHelper = new DatabaseHelper(this);
        userId = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).getInt(KEY_USER_ID, -1);

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tvGoalSummary = findViewById(R.id.tvGoalSummary);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tableWeights = findViewById(R.id.tableWeights);
        Button btnAddWeight = findViewById(R.id.btnAddWeight);
        Button btnGoalSettings = findViewById(R.id.btnGoalSettings);
        Button btnSmsSettings = findViewById(R.id.btnSmsSettings);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnAddWeight.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddWeightActivity.class);
            startActivity(intent);
        });

        btnGoalSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, GoalWeightActivity.class);
            startActivity(intent);
        });

        btnSmsSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SmsSettingsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoalSummary();
        loadWeightGrid();
    }

    /**
     * Loads the goal summary from SQLite and shows it on screen.
     */
    private void loadGoalSummary() {
        Double goalWeight = databaseHelper.getGoalWeight(userId);
        if (goalWeight == null) {
            tvGoalSummary.setText("Goal Weight: No goal saved yet");
        } else {
            tvGoalSummary.setText(String.format(Locale.US, "Goal Weight: %.1f lb", goalWeight));
        }
    }

    /**
     * Builds the table-style grid using all weight entries from SQLite.
     */
    private void loadWeightGrid() {
        while (tableWeights.getChildCount() > 1) {
            tableWeights.removeViewAt(1);
        }

        List<WeightEntry> entries = databaseHelper.getAllWeightsForUser(userId);
        tvEmptyState.setVisibility(entries.isEmpty() ? TextView.VISIBLE : TextView.GONE);

        for (WeightEntry entry : entries) {
            TableRow row = new TableRow(this);
            row.setPadding(0, 8, 0, 8);

            TextView tvDate = new TextView(this);
            tvDate.setPadding(16, 16, 16, 16);
            tvDate.setText(entry.getEntryDate());

            TextView tvWeight = new TextView(this);
            tvWeight.setPadding(16, 16, 16, 16);
            tvWeight.setText(String.format(Locale.US, "%.1f lb", entry.getWeight()));

            Button btnEdit = new Button(this);
            btnEdit.setText("Edit");
            btnEdit.setOnClickListener(v -> openEditScreen(entry.getId()));

            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setOnClickListener(v -> deleteWeight(entry.getId()));

            row.addView(tvDate);
            row.addView(tvWeight);
            row.addView(btnEdit);
            row.addView(btnDelete);
            tableWeights.addView(row);
        }
    }

    /**
     * Opens the AddWeight screen in edit mode.
     */
    private void openEditScreen(int weightId) {
        Intent intent = new Intent(DashboardActivity.this, AddWeightActivity.class);
        intent.putExtra("weight_id", weightId);
        startActivity(intent);
    }

    /**
     * Deletes a weight entry and refreshes the grid.
     */
    private void deleteWeight(int weightId) {
        int rowsDeleted = databaseHelper.deleteWeight(weightId, userId);
        if (rowsDeleted > 0) {
            Toast.makeText(this, "Weight entry deleted.", Toast.LENGTH_SHORT).show();
            loadWeightGrid();
        } else {
            Toast.makeText(this, "Unable to delete the selected entry.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clears the current session and returns to the login screen.
     */
    private void logoutUser() {
        SharedPreferences preferences = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE);
        preferences.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

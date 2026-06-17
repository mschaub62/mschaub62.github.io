package com.example.weighttrackingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * GoalWeightActivity allows the user to create or update the goal weight.
 */
public class GoalWeightActivity extends AppCompatActivity {

    private static final String SESSION_PREFS = "weight_tracker_session";
    private static final String KEY_USER_ID = "user_id";

    private EditText etGoalWeight;
    private WeightRepository repository;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_weight);

        repository = new WeightRepository(this);
        userId = getSharedPreferences(SESSION_PREFS, MODE_PRIVATE).getInt(KEY_USER_ID, -1);

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        etGoalWeight = findViewById(R.id.etGoalWeight);
        Button btnSaveGoal = findViewById(R.id.btnSaveGoal);

        loadExistingGoal();
        btnSaveGoal.setOnClickListener(v -> saveGoalWeight());
    }

    /**
     * Displays the current saved goal weight in the form.
     */
    private void loadExistingGoal() {
        Double currentGoal = repository.getGoalWeight(userId);
        if (currentGoal != null) {
            etGoalWeight.setText(String.format(Locale.US, "%.1f", currentGoal));
        }
    }

    /**
     * Saves the goal weight to SQLite after applying the shared weight validation rule.
     */
    private void saveGoalWeight() {
        String goalWeightText = etGoalWeight.getText().toString().trim();

        if (goalWeightText.isEmpty()) {
            Toast.makeText(this, "Please enter a goal weight.", Toast.LENGTH_SHORT).show();
            return;
        }

        double goalWeightValue;
        try {
            goalWeightValue = Double.parseDouble(goalWeightText);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Enter a valid numeric goal weight.", Toast.LENGTH_SHORT).show();
            return;
        }

        WeightValidator.ValidationResult validationResult = WeightValidator.validateWeight(goalWeightValue);
        if (!validationResult.isValid()) {
            Toast.makeText(this, validationResult.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        long result = repository.saveGoalWeight(userId, goalWeightValue);
        if (result != -1) {
            Toast.makeText(this, "Goal weight saved.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(GoalWeightActivity.this, DashboardActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Unable to save goal weight.", Toast.LENGTH_SHORT).show();
        }
    }
}

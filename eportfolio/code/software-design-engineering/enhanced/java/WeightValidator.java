package com.example.weighttrackingapplication;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Centralized validation rules keep user input checks consistent across screens.
 */
public final class WeightValidator {
    public static final double MIN_WEIGHT = 20.0;
    public static final double MAX_WEIGHT = 1000.0;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 30;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+() .-]{7,20}$");

    private WeightValidator() {
        // Utility class. Do not instantiate.
    }

    public static ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.invalid("Please enter a username.");
        }
        String trimmed = username.trim();
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("Username must be 3-30 characters and use letters, numbers, dots, dashes, or underscores only.");
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.invalid("Please enter a password.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.invalid("Password must be at least 8 characters long.");
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateWeight(double weight) {
        if (Double.isNaN(weight) || Double.isInfinite(weight)) {
            return ValidationResult.invalid("Enter a valid numeric weight.");
        }
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            return ValidationResult.invalid(String.format(Locale.US, "Weight must be between %.0f and %.0f pounds.", MIN_WEIGHT, MAX_WEIGHT));
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ValidationResult.invalid("Enter a phone number first.");
        }
        if (!PHONE_PATTERN.matcher(phoneNumber.trim()).matches()) {
            return ValidationResult.invalid("Enter a valid phone number using 7-20 digits or common phone symbols.");
        }
        return ValidationResult.valid();
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}

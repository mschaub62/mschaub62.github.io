package com.example.weighttrackingapplication;

/**
 * Immutable model for one user's SMS notification preferences.
 */
public class SmsSettings {
    private final String phoneNumber;
    private final boolean enabled;

    public SmsSettings(String phoneNumber, boolean enabled) {
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber;
        this.enabled = enabled;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

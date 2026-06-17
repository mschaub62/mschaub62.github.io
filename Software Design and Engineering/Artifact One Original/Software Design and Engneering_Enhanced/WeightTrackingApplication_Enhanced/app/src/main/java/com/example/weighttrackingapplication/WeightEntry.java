package com.example.weighttrackingapplication;

/**
 * WeightEntry stores one weight record from the database.
 */
public class WeightEntry {
    private final int id;
    private final String entryDate;
    private final double weight;

    public WeightEntry(int id, String entryDate, double weight) {
        this.id = id;
        this.entryDate = entryDate;
        this.weight = weight;
    }

    public int getId() {
        return id;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public double getWeight() {
        return weight;
    }
}

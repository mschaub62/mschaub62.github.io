package com.example.weighttrackingapplication;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeightValidatorTest {
    @Test
    public void validateWeight_acceptsReasonableWeight() {
        assertTrue(WeightValidator.validateWeight(185.5).isValid());
    }

    @Test
    public void validateWeight_rejectsOutOfRangeWeight() {
        assertFalse(WeightValidator.validateWeight(5.0).isValid());
        assertFalse(WeightValidator.validateWeight(1500.0).isValid());
    }

    @Test
    public void validateUsername_rejectsUnsafeCharacters() {
        assertFalse(WeightValidator.validateUsername("bad user!").isValid());
    }

    @Test
    public void validatePhoneNumber_acceptsCommonPhoneFormat() {
        assertTrue(WeightValidator.validatePhoneNumber("+1 (555) 123-4567").isValid());
    }
}

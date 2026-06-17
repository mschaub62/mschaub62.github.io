package com.example.weighttrackingapplication;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordUtilsTest {
    @Test
    public void hashPassword_verifiesOriginalPassword() {
        PasswordUtils.PasswordRecord record = PasswordUtils.hashPassword("SecurePass123");
        assertTrue(PasswordUtils.verifyPassword("SecurePass123", record.getHash(), record.getSalt()));
    }

    @Test
    public void hashPassword_rejectsIncorrectPassword() {
        PasswordUtils.PasswordRecord record = PasswordUtils.hashPassword("SecurePass123");
        assertFalse(PasswordUtils.verifyPassword("WrongPassword", record.getHash(), record.getSalt()));
    }

    @Test
    public void hashPassword_usesUniqueSalt() {
        PasswordUtils.PasswordRecord first = PasswordUtils.hashPassword("SecurePass123");
        PasswordUtils.PasswordRecord second = PasswordUtils.hashPassword("SecurePass123");
        assertNotEquals(first.getHash(), second.getHash());
    }
}

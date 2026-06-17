package com.example.weighttrackingapplication;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PasswordUtils hashes and verifies passwords before they are stored in SQLite.
 *
 * This replaces the original plain-text password storage with salted PBKDF2 hashes.
 * The app stores the salt and the derived hash, never the user's original password.
 */
public final class PasswordUtils {
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 120_000;

    private PasswordUtils() {
        // Utility class. Do not instantiate.
    }

    public static PasswordRecord hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = deriveHash(password, salt);
        return new PasswordRecord(toHex(hash), toHex(salt));
    }

    public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
        if (password == null || storedHash == null || storedSalt == null) {
            return false;
        }

        try {
            byte[] salt = fromHex(storedSalt);
            byte[] expectedHash = fromHex(storedHash);
            byte[] actualHash = deriveHash(password, salt);
            return Arrays.equals(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] deriveHash(String password, byte[] salt) {
        try {
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            return factory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Unable to hash password securely.", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value));
        }
        return builder.toString();
    }

    private static byte[] fromHex(String hexValue) {
        if (hexValue.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hexadecimal value.");
        }

        byte[] output = new byte[hexValue.length() / 2];
        for (int i = 0; i < hexValue.length(); i += 2) {
            output[i / 2] = (byte) Integer.parseInt(hexValue.substring(i, i + 2), 16);
        }
        return output;
    }

    public static final class PasswordRecord {
        private final String hash;
        private final String salt;

        public PasswordRecord(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }

        public String getHash() {
            return hash;
        }

        public String getSalt() {
            return salt;
        }
    }
}

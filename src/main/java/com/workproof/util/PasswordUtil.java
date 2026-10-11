package com.workproof.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing with BCrypt (spec section 4.1).
 *
 * - Passwords are NEVER stored or logged in plain text.
 * - BCrypt salts every hash automatically, so two users with the same password
 *   get different hashes.
 * - Cost factor 12 is a good balance for a college project (roughly 250 ms/hash).
 * - BCrypt has a 72-byte input limit; we reject longer passwords explicitly so a
 *   user cannot silently get "truncated password" behaviour.
 */
public final class PasswordUtil {

    private static final int COST = 12;
    private static final int MAX_BCRYPT_BYTES = 72;

    private PasswordUtil() {
        // utility class
    }

    /** Hashes a plain-text password. Returns a 60-character BCrypt hash. */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        if (plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalArgumentException("Password is too long (max 72 bytes)");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    /**
     * Verifies a login attempt. Uses a constant-ish time check and never
     * throws on malformed stored hashes - it just returns false.
     */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Stored value is not a valid BCrypt hash (e.g. legacy plaintext row).
            return false;
        }
    }
}

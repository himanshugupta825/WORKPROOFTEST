package com.workproof.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Password storage: hashes must be salted, never reversible, and verifiable.
 */
class PasswordUtilTest {

    @Test
    @DisplayName("a stored hash never contains the password and uses BCrypt")
    void hashIsSafe() {
        String plain = "Member@123";
        String hash = PasswordUtil.hash(plain);

        assertNotEquals(plain, hash, "the password must never be stored as-is");
        assertFalse(hash.contains(plain), "the hash must not embed the plaintext");
        assertTrue(hash.startsWith("$2a$"), "jBCrypt produces $2a$ hashes, found: " + hash.substring(0, 4));
        assertTrue(hash.length() >= 59, "a BCrypt hash is at least 59 characters");
    }

    @Test
    @DisplayName("the same password hashes differently every time (random salt)")
    void saltMakesEveryHashUnique() {
        String first = PasswordUtil.hash("Member@123");
        String second = PasswordUtil.hash("Member@123");

        assertNotEquals(first, second, "without a random salt two users would share a hash");
        assertTrue(PasswordUtil.matches("Member@123", first));
        assertTrue(PasswordUtil.matches("Member@123", second));
    }

    @Test
    @DisplayName("verification accepts the right password and rejects everything else")
    void verification() {
        String hash = PasswordUtil.hash("Reviewer@123");

        assertTrue(PasswordUtil.matches("Reviewer@123", hash));
        assertFalse(PasswordUtil.matches("reviewer@123", hash), "passwords are case sensitive");
        assertFalse(PasswordUtil.matches("Reviewer@1234", hash));
        assertFalse(PasswordUtil.matches("", hash));
    }

    @Test
    @DisplayName("a corrupt or missing hash fails closed instead of throwing")
    void corruptHashFailsClosed() {
        assertFalse(PasswordUtil.matches("anything", null));
        assertFalse(PasswordUtil.matches("anything", ""));
        assertFalse(PasswordUtil.matches("anything", "not-a-bcrypt-hash"));
    }

    @Test
    @DisplayName("an empty password is refused before it is ever hashed")
    void emptyPasswordRefused() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }

    @Test
    @DisplayName("BCrypt's 72-byte limit is enforced instead of silently truncating")
    void tooLongPasswordRefused() {
        String exactly72 = "a".repeat(72);
        assertTrue(PasswordUtil.matches("a".repeat(72), PasswordUtil.hash(exactly72)),
                "72 bytes is still allowed");

        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("a".repeat(73)),
                "a longer password must be rejected, never truncated");
    }
}

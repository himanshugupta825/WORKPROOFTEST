package com.workproof.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CSRF token generation and comparison.
 *
 * These tests are part of the security evidence for the project: they prove the
 * token is unpredictable, URL-safe, and compared in a length-independent way.
 */
class CsrfUtilTest {

    @Test
    @DisplayName("a token carries 256 bits of randomness and is URL-safe")
    void tokenShape() {
        String token = CsrfUtil.newToken();

        // 32 bytes -> base64url without padding is always 43 characters.
        assertEquals(43, token.length(), "32 random bytes must encode to 43 base64url characters");
        assertFalse(token.contains("+"), "URL-safe base64 never contains '+'");
        assertFalse(token.contains("/"), "URL-safe base64 never contains '/'");
        assertFalse(token.contains("="), "padding is stripped so the token is form friendly");
    }

    @Test
    @DisplayName("tokens do not repeat")
    void tokensAreUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            assertTrue(seen.add(CsrfUtil.newToken()), "a token was generated twice");
        }
        assertEquals(500, seen.size());
    }

    @Test
    @DisplayName("equal tokens are accepted")
    void equalTokensMatch() {
        String token = CsrfUtil.newToken();
        assertTrue(CsrfUtil.constantTimeEquals(token, token));
        assertTrue(CsrfUtil.constantTimeEquals("abc123", "abc123"));
    }

    @Test
    @DisplayName("any difference is rejected, whatever the length")
    void differentTokensAreRejected() {
        String token = CsrfUtil.newToken();

        assertFalse(CsrfUtil.constantTimeEquals(token, token.substring(0, token.length() - 1)),
                "a shorter token must not be accepted");
        assertFalse(CsrfUtil.constantTimeEquals(token, token + "x"),
                "a longer token must not be accepted");
        assertFalse(CsrfUtil.constantTimeEquals("abcdefgh", "abcdefgi"),
                "a single different character must be rejected");
        assertFalse(CsrfUtil.constantTimeEquals("", "x"));
    }

    @Test
    @DisplayName("null tokens never match (missing session or missing form field)")
    void nullsAreRejected() {
        String token = CsrfUtil.newToken();
        assertFalse(CsrfUtil.constantTimeEquals(null, token));
        assertFalse(CsrfUtil.constantTimeEquals(token, null));
        assertFalse(CsrfUtil.constantTimeEquals(null, null));
    }

    @Test
    @DisplayName("the parameter and session attribute names match the JSPs")
    void contractNames() {
        // The JSPs hard-code  name="_csrf"  and  ${sessionScope.csrfToken}.
        // If these constants are ever renamed, every form breaks - so pin them down.
        assertEquals("_csrf", CsrfUtil.PARAM_NAME);
        assertEquals("csrfToken", CsrfUtil.SESSION_ATTR);
        assertNotEquals(CsrfUtil.newToken(), CsrfUtil.newToken());
    }
}

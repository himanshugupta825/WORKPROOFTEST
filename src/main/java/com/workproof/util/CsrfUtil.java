package com.workproof.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cross-Site Request Forgery (CSRF) protection - synchroniser token pattern.
 *
 * How it works:
 *   1. Every logged-in session gets one random token ({@link #getToken}).
 *   2. Every state-changing form (POST) embeds it in a hidden input
 *      (<code>&lt;input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"&gt;</code>).
 *   3. {@link com.workproof.filter.CsrfFilter} compares the posted value with the
 *      session value using a constant-time comparison and rejects mismatches with HTTP 403.
 *
 * GET requests are never used to change data, so they are not token-protected.
 */
public final class CsrfUtil {

    /** Form field / request parameter name. Keep in sync with the JSPs. */
    public static final String PARAM_NAME = "_csrf";

    /** Session attribute name. Keep in sync with the JSPs. */
    public static final String SESSION_ATTR = "csrfToken";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private CsrfUtil() {
        // utility class
    }

    /** Returns the existing token for this session, creating one when needed. */
    public static String getToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_ATTR);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }
        String token = newToken();
        session.setAttribute(SESSION_ATTR, token);
        return token;
    }

    public static String getToken(HttpServletRequest request) {
        return getToken(request.getSession(true));
    }

    /** Generates a fresh 256-bit token (URL-safe Base64). */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /**
     * Compares the submitted token with the session token.
     * Always runs the full comparison to avoid timing leaks, and tolerates nulls.
     */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object stored = session.getAttribute(SESSION_ATTR);
        String submitted = request.getParameter(PARAM_NAME);
        if (!(stored instanceof String) || submitted == null) {
            return false;
        }
        return constantTimeEquals((String) stored, submitted);
    }

    /** Length-independent, constant-time string comparison. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        int diff = a.length() ^ b.length();
        int max = Math.max(a.length(), b.length());
        for (int i = 0; i < max; i++) {
            char ca = i < a.length() ? a.charAt(i) : 0;
            char cb = i < b.length() ? b.charAt(i) : 0;
            diff |= (ca ^ cb);
        }
        return diff == 0;
    }
}

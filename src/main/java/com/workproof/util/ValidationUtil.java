package com.workproof.util;

import com.workproof.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Server-side validation helpers.
 *
 * Rule of the project: EVERY form is validated on the server, even when the
 * browser also validates with HTML5 attributes. Client-side checks are only a
 * convenience for the user - they are never a security control.
 *
 * All methods return a trimmed, safe value or throw {@link ValidationException}
 * with a message that is safe to show to the user.
 */
public final class ValidationUtil {

    /** Deliberately small, permissive e-mail pattern (full RFC validation is a rabbit hole). */
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\-\\s()]{0,20}$");
    private static final Pattern PROJECT_CODE = Pattern.compile("^[A-Za-z0-9\\-]{2,20}$");

    private ValidationUtil() {
        // utility class
    }

    /* ---------------------------------------------------------------- text */

    public static String require(String value, String fieldLabel) {
        String v = trimToNull(value);
        if (v == null) {
            throw new ValidationException(fieldLabel + " is required.");
        }
        return v;
    }

    public static String require(String value, String fieldLabel, int min, int max) {
        String v = require(value, fieldLabel);
        if (v.length() < min) {
            throw new ValidationException(fieldLabel + " must be at least " + min + " characters.");
        }
        if (v.length() > max) {
            throw new ValidationException(fieldLabel + " must be at most " + max + " characters.");
        }
        return v;
    }

    public static String optional(String value, int max, String fieldLabel) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        if (v.length() > max) {
            throw new ValidationException(fieldLabel + " must be at most " + max + " characters.");
        }
        return v;
    }

    /* --------------------------------------------------------------- email */

    public static String requireEmail(String value) {
        String v = require(value, "Email", 5, 150).toLowerCase();
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        return v;
    }

    public static String optionalPhone(String value) {
        String v = optional(value, 20, "Phone");
        if (v == null) {
            return null;
        }
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException("Phone may contain digits, spaces and + - ( ) only.");
        }
        return v;
    }

    /* ------------------------------------------------------------ password */

    /**
     * Password policy: at least 8 characters, with at least one letter and one digit.
     * Kept simple and explainable for a viva, but stronger than "1234".
     */
    public static String requirePassword(String value, String fieldLabel) {
        String v = value == null ? "" : value;
        if (v.length() < 8) {
            throw new ValidationException(fieldLabel + " must be at least 8 characters.");
        }
        if (v.length() > 72) {
            throw new ValidationException(fieldLabel + " must be at most 72 characters.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : v.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new ValidationException(fieldLabel + " must contain at least one letter and one digit.");
        }
        return v;
    }

    public static void requireMatch(String a, String b, String message) {
        if (a == null || !a.equals(b)) {
            throw new ValidationException(message);
        }
    }

    /* --------------------------------------------------------------- dates */

    public static LocalDate requireDate(String value, String fieldLabel) {
        String v = trimToNull(value);
        if (v == null) {
            throw new ValidationException(fieldLabel + " is required.");
        }
        try {
            return LocalDate.parse(v);           // ISO yyyy-MM-dd, what <input type="date"> posts
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldLabel + " must be a valid date (yyyy-MM-dd).");
        }
    }

    public static LocalDate optionalDate(String value, String fieldLabel) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        return requireDate(v, fieldLabel);
    }

    /** Validates a project's date window: end date must not be before start date. */
    public static void validateDateRange(LocalDate start, LocalDate end, String startLabel, String endLabel) {
        if (start == null || end == null) {
            throw new ValidationException(startLabel + " and " + endLabel + " are both required.");
        }
        if (end.isBefore(start)) {
            throw new ValidationException(endLabel + " cannot be before " + startLabel + ".");
        }
    }

    /* -------------------------------------------------------------- numbers */

    public static int requireInt(String value, String fieldLabel) {
        String v = trimToNull(value);
        if (v == null) {
            throw new ValidationException(fieldLabel + " is required.");
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldLabel + " must be a number.");
        }
    }

    public static int optionalInt(String value, int fallback, int min, int max) {
        String v = trimToNull(value);
        if (v == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(v);
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /* ------------------------------------------------------------ combo box */

    /** Ensures a posted value is one of the values offered by the form (whitelist). */
    public static String requireOneOf(String value, String fieldLabel, String... allowed) {
        String v = require(value, fieldLabel);
        for (String a : allowed) {
            if (a.equals(v)) {
                return v;
            }
        }
        throw new ValidationException(fieldLabel + " has an invalid value.");
    }

    public static String requireProjectCode(String value) {
        String v = require(value, "Project code", 2, 20).toUpperCase();
        if (!PROJECT_CODE.matcher(v).matches()) {
            throw new ValidationException("Project code may contain letters, digits and hyphens only.");
        }
        return v;
    }

    /** Small helper so callers can write shorter code. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }
}

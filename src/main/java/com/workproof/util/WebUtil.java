package com.workproof.util;

import com.workproof.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared helpers for servlets: session user access, flash messages,
 * JSP forwarding, safe parameter reading and CSV escaping.
 */
public final class WebUtil {

    public static final String SESSION_USER = "currentUser";
    public static final String FLASH_SUCCESS = "flashSuccess";
    public static final String FLASH_ERROR = "flashError";

    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private WebUtil() {
        // utility class
    }

    /* ---------------------------------------------------------- session user */

    /** @return the logged-in user, or null when there is no valid session. */
    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(SESSION_USER);
        return user instanceof User ? (User) user : null;
    }

    /* ------------------------------------------------------------- client ip */

    /**
     * Best-effort client IP for the audit log.
     * X-Forwarded-For is only used when the app runs behind a proxy; the value is
     * validated to look like an IP address so a malicious header cannot inject
     * arbitrary text into the log.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (isIpLike(first)) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    private static boolean isIpLike(String value) {
        if (value.length() > 45) {
            return false;
        }
        return value.matches("[0-9a-fA-F:.]+");
    }

    /* ---------------------------------------------------------------- flash */

    /** Stores a one-time success message shown on the next rendered page. */
    public static void flashSuccess(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_SUCCESS, message);
    }

    /** Stores a one-time error message shown on the next rendered page. */
    public static void flashError(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_ERROR, message);
    }

    /* ----------------------------------------------------------- navigation */

    /** Forwards to a JSP inside /WEB-INF/views (never directly reachable by URL). */
    public static void forward(HttpServletRequest request, HttpServletResponse response, String viewPath)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + viewPath).forward(request, response);
    }

    public static void redirect(HttpServletResponse response, HttpServletRequest request, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /* ------------------------------------------------------------ parameters */

    /** Reads a form field, trimmed, never null. */
    public static String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    public static Integer intParam(HttpServletRequest request, String name) {
        String raw = param(request, name);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Like {@link #intParam} but throws when required - the caller passes a field label. */
    public static int requiredIntParam(HttpServletRequest request, String name, String label) {
        Integer value = intParam(request, name);
        if (value == null) {
            throw new com.workproof.exception.ValidationException(label + " is required.");
        }
        return value;
    }

    public static LocalDate dateParam(HttpServletRequest request, String name) {
        String raw = param(request, name);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new com.workproof.exception.ValidationException("Invalid date value: " + raw);
        }
    }

    /**
     * Lenient variant of {@link #dateParam} used by report/table FILTERS: a mistyped
     * value is ignored (the filter simply does not apply) instead of failing the page.
     * Forms that must validate dates keep using {@link #dateParam}.
     */
    public static LocalDate dateParamOrNull(HttpServletRequest request, String name) {
        String raw = param(request, name);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Reads the "page" query parameter as a 1-based page number (defaults to 1). */
    public static int pageParam(HttpServletRequest request) {
        return ValidationUtil.optionalInt(param(request, "page"), 1, 1, 10_000);
    }

    public static boolean boolParam(HttpServletRequest request, String name) {
        String raw = param(request, name);
        return "true".equalsIgnoreCase(raw) || "on".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    /* ------------------------------------------------------------------ CSV */

    /**
     * Escapes one CSV cell: wraps in quotes when needed and doubles inner quotes.
     * A leading =, +, - or @ is prefixed with ' so spreadsheet software does not
     * interpret user input as a formula (CSV injection).
     */
    public static String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        v = v.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    /**
     * Sets the headers for a CSV download and returns the writer to fill.
     *
     * The UTF-8 byte-order mark is written through the SAME writer (never through
     * response.getOutputStream(), which would make getWriter() throw
     * IllegalStateException). Excel needs the BOM to detect UTF-8 accents.
     */
    public static PrintWriter prepareCsvDownload(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        PrintWriter out = response.getWriter();
        out.write('\uFEFF');
        return out;
    }

    /** Timestamp string used inside generated file names. */
    public static String timestampForFilename() {
        return java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}

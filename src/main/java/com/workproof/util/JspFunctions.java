package com.workproof.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Helper functions exposed to JSP pages through the WorkProof tag library
 * (see WEB-INF/tlds/workproof.tld). Usage inside a JSP:
 *
 *   &lt;%@ taglib prefix="wp" uri="http://workproof.com/tags" %&gt;
 *   ${wp:date(task.dueDate)}        -> 11 Oct 2026
 *   ${wp:dateTime(review.reviewedAt)} -> 11 Oct 2026, 14:30
 *
 * Why a tag library instead of code in the JSP?
 *   - JSTL's fmt:formatDate only understands java.util.Date, not java.time types,
 *   - and formatting logic inside scriptlets would mix view code with Java code,
 *     which the project explicitly forbids.
 */
public final class JspFunctions {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private JspFunctions() {
        // utility class
    }

    /** Formats a LocalDate / LocalDateTime / java.util.Date as "11 Oct 2026". */
    public static String date(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof LocalDate) {
            return DATE.format((LocalDate) value);
        }
        if (value instanceof LocalDateTime) {
            return DATE.format(((LocalDateTime) value).toLocalDate());
        }
        if (value instanceof java.util.Date) {
            return DATE.format(((java.util.Date) value).toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        }
        return String.valueOf(value);
    }

    /** Formats a date-time as "11 Oct 2026, 14:30". */
    public static String dateTime(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof LocalDateTime) {
            return DATE_TIME.format((LocalDateTime) value);
        }
        if (value instanceof LocalDate) {
            return DATE.format((LocalDate) value);
        }
        if (value instanceof java.util.Date) {
            return DATE_TIME.format(java.time.LocalDateTime.ofInstant(
                    ((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault()));
        }
        return String.valueOf(value);
    }

    public static String time(Object value) {
        if (value instanceof LocalDateTime) {
            return TIME.format((LocalDateTime) value);
        }
        return "";
    }

    /** "2 hours ago" / "3 days ago" / "-" for the activity feeds. */
    public static String timeAgo(Object value) {
        if (!(value instanceof LocalDateTime)) {
            return "-";
        }
        LocalDateTime moment = (LocalDateTime) value;
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(moment, now);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = ChronoUnit.HOURS.between(moment, now);
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = ChronoUnit.DAYS.between(moment, now);
        if (days < 30) return days + " day" + (days == 1 ? "" : "s") + " ago";
        return DATE.format(moment.toLocalDate());
    }

    /** First letter(s) used inside the avatar circles. */
    public static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return sb.toString();
    }

    /** 1536 -> "1.5 KB". */
    public static String fileSize(Object value) {
        if (!(value instanceof Number)) {
            return "-";
        }
        return FileStorageUtil.humanReadableSize(((Number) value).longValue());
    }

    /** Shortens long text for table cells. */
    public static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String single = text.replace('\n', ' ').replace('\r', ' ').trim();
        return single.length() <= max ? single : single.substring(0, Math.max(1, max - 1)) + "…";
    }

    /** Formats a percentage shown next to progress bars. */
    public static String percent(Object value) {
        if (!(value instanceof Number)) {
            return "0%";
        }
        double number = ((Number) value).doubleValue();
        return String.format(java.util.Locale.ROOT, "%.0f%%", number);
    }

    /** Days between today and a date, e.g. "in 4 days" / "3 days ago". */
    public static String daysFromToday(Object value) {
        LocalDate date = null;
        if (value instanceof LocalDate) {
            date = (LocalDate) value;
        } else if (value instanceof LocalDateTime) {
            date = ((LocalDateTime) value).toLocalDate();
        }
        if (date == null) {
            return "-";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days == 0) return "today";
        if (days > 0) return "in " + days + " day" + (days == 1 ? "" : "s");
        return Math.abs(days) + " day" + (Math.abs(days) == 1 ? "" : "s") + " ago";
    }

    /** Percent of a task count, defensive against division by zero. */
    public static long ratioPercent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(100.0 * part / total);
    }
}

package com.workproof.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CSV export escaping.
 *
 * Two risks are covered here:
 *   1. a comma, quote or newline inside a task title must not shift the columns;
 *   2. a value starting with = + - or @ must not be executed as a formula when the
 *      examiner opens the exported file in Excel (CSV injection).
 */
class WebUtilCsvTest {

    @Test
    @DisplayName("plain values are quoted")
    void plainValues() {
        assertEquals("\"Complaint Portal\"", WebUtil.csv("Complaint Portal"));
    }

    @Test
    @DisplayName("an empty value becomes an empty quoted cell, null is not printed as text")
    void emptyValues() {
        assertEquals("\"\"", WebUtil.csv(""));
        assertEquals("\"\"", WebUtil.csv(null));
    }

    @Test
    @DisplayName("quotes are doubled and commas are contained")
    void quotesAndCommas() {
        assertEquals("\"Report, final\"", WebUtil.csv("Report, final"));
        assertEquals("\"a\"\"b\"", WebUtil.csv("a\"b"));
    }

    @Test
    @DisplayName("formula starters are neutralised with a leading apostrophe")
    void formulaInjectionIsBlocked() {
        assertEquals("\"'=1+1\"", WebUtil.csv("=1+1"));
        assertEquals("\"'+SUM(A1:A9)\"", WebUtil.csv("+SUM(A1:A9)"));
        assertEquals("\"'-2+3\"", WebUtil.csv("-2+3"));
        assertEquals("\"'@SUM(A1)\"", WebUtil.csv("@SUM(A1)"));

        // A value that merely CONTAINS those characters is left alone.
        assertEquals("\"2+2=4\"", WebUtil.csv("2+2=4"));
    }

    @Test
    @DisplayName("newlines survive inside a quoted cell")
    void newlines() {
        assertEquals("\"line one\nline two\"", WebUtil.csv("line one\nline two"));
    }

    @Test
    @DisplayName("exported file names are filesystem safe and unique")
    void exportFileNames() {
        String name = WebUtil.timestampForFilename();

        assertTrue(name.matches("\\d{8}-\\d{6}"), "expected yyyyMMdd-HHmmss but was " + name);
        assertFalse(name.contains(":"), "colons are illegal in Windows file names");
        assertFalse(name.contains("/"));
    }
}

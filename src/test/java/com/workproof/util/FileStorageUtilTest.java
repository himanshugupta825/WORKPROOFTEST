package com.workproof.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Upload safety helpers.
 *
 * The stored file name is generated, never taken from the browser, and the
 * extension whitelist is what stops someone from dropping a .exe or a .jsp into
 * the evidence folder. These tests document exactly which extensions are allowed.
 */
class FileStorageUtilTest {

    @Test
    @DisplayName("extension detection is case-insensitive and handles dots correctly")
    void extensionOf() {
        assertEquals("png", FileStorageUtil.extensionOf("screenshot.PNG"));
        assertEquals("pdf", FileStorageUtil.extensionOf("report.final.pdf"));
        assertEquals("gz", FileStorageUtil.extensionOf("evidence.tar.gz"), "only the last part counts");
        assertEquals("", FileStorageUtil.extensionOf("noextension"));
        assertEquals("", FileStorageUtil.extensionOf("trailing."));
        assertEquals("", FileStorageUtil.extensionOf(null));
    }

    @Test
    @DisplayName("evidence types a student really uploads are allowed")
    void allowedTypes() {
        String[] allowed = {"shot.png", "photo.jpg", "photo.jpeg", "anim.gif", "design.webp",
                "report.pdf", "notes.txt", "data.csv", "readme.md", "plan.doc", "plan.docx",
                "sheet.xls", "sheet.xlsx", "deck.ppt", "deck.pptx", "archive.zip", "idea.log", "demo.mp4"};

        for (String name : allowed) {
            assertTrue(FileStorageUtil.isAllowedExtension(name), name + " should be uploadable");
        }
    }

    @Test
    @DisplayName("executables, scripts and server pages are refused")
    void dangerousTypesRejected() {
        String[] blocked = {"payload.exe", "run.bat", "run.cmd", "script.sh", "hook.php",
                "shell.jsp", "evil.jspx", "lib.so", "app.jar", "installer.msi", "macro.jar",
                "backdoor.pl", "server.py", "config.yml", "noextension"};

        for (String name : blocked) {
            assertFalse(FileStorageUtil.isAllowedExtension(name), name + " must be rejected");
        }
    }

    @Test
    @DisplayName("the accept attribute sent to the browser never lists a blocked type")
    void acceptAttributeMatchesWhitelist() {
        String accept = FileStorageUtil.acceptedExtensionsForInput();

        assertTrue(accept.startsWith("."), "should look like .png,.jpg,...");
        assertFalse(accept.contains("exe"), "an executable must never be offered in the file picker");
        assertFalse(accept.contains("jsp"), "a server page must never be offered in the file picker");
        assertTrue(accept.contains(".pdf"));
    }

    @Test
    @DisplayName("a browser-supplied path is reduced to a safe display name")
    void displayNameSanitising() {
        assertEquals("passwd", FileStorageUtil.sanitiseDisplayName("../../etc/passwd"),
                "path traversal is stripped, only the base name survives");
        assertEquals("report.pdf", FileStorageUtil.sanitiseDisplayName("C:\\Users\\me\\report.pdf"));
        assertEquals("evidence.png", FileStorageUtil.sanitiseDisplayName("evidence\u0000.png"),
                "control characters are removed");
        assertEquals("evidence", FileStorageUtil.sanitiseDisplayName("   "),
                "an empty name still produces a usable one");
        assertTrue(FileStorageUtil.sanitiseDisplayName("x".repeat(400)).length() <= 200,
                "the display name must fit the database column");
    }

    @Test
    @DisplayName("download sizes are formatted for humans")
    void readableSizes() {
        assertEquals("512 B", FileStorageUtil.humanReadableSize(512));
        assertEquals("2.0 KB", FileStorageUtil.humanReadableSize(2048));
        assertEquals("5.00 MB", FileStorageUtil.humanReadableSize(5L * 1024 * 1024));
    }

    @Test
    @DisplayName("SHA-256 of the stored bytes is recorded for the audit trail")
    void sha256() {
        // Well-known test vector for the string "abc".
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                FileStorageUtil.sha256Of("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toLowerCase());

        assertNotEquals(FileStorageUtil.sha256Of("abc".getBytes()),
                FileStorageUtil.sha256Of("abd".getBytes()),
                "a one character change must change the digest");
    }
}

package com.workproof.util;

import com.workproof.exception.ValidationException;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Secure file-upload handling for proof-of-work evidence (spec section 8).
 *
 * Security measures implemented here:
 *   - Extension whitelist AND magic-byte (file signature) check, so renaming
 *     "malware.exe" to "notes.pdf" is rejected.
 *   - Server-side size limit (independent of the browser).
 *   - Random generated storage name (UUID) - the user-supplied name is never
 *     used as a path, which removes path traversal ("../../web.xml") entirely.
 *   - Files are written outside the web application root, so they can never be
 *     executed or fetched directly by the browser.
 *   - SHA-256 checksum is stored with the metadata for integrity checking.
 */
public final class FileStorageUtil {

    /** Allowed extensions (lower-case, without dot). */
    private static final Set<String> ALLOWED_EXTENSIONS = new LinkedHashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "webp",      // screenshots
            "pdf", "doc", "docx", "xls", "xlsx",      // documents
            "ppt", "pptx", "txt", "csv", "md",        // notes / data
            "zip", "log", "mp4"                       // archives, logs, short demo videos
    ));

    /**
     * Magic-byte signatures for the file types we accept.
     * Extensions not listed here (txt, csv, md, log) are text formats without a
     * fixed signature and are validated by extension only.
     */
    private static final Map<String, List<byte[]>> SIGNATURES = new LinkedHashMap<>();

    static {
        SIGNATURES.put("png", List.of(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}));
        SIGNATURES.put("jpg", List.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        SIGNATURES.put("jpeg", List.of(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        SIGNATURES.put("gif", List.of("GIF87a".getBytes(), "GIF89a".getBytes()));
        SIGNATURES.put("webp", List.of(new byte[]{'R', 'I', 'F', 'F'}));
        SIGNATURES.put("pdf", List.of("%PDF".getBytes()));
        SIGNATURES.put("doc", List.of(new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}));
        SIGNATURES.put("xls", List.of(new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}));
        SIGNATURES.put("ppt", List.of(new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}));
        SIGNATURES.put("docx", List.of(new byte[]{'P', 'K', 0x03, 0x04}));
        SIGNATURES.put("xlsx", List.of(new byte[]{'P', 'K', 0x03, 0x04}));
        SIGNATURES.put("pptx", List.of(new byte[]{'P', 'K', 0x03, 0x04}));
        SIGNATURES.put("zip", List.of(new byte[]{'P', 'K', 0x03, 0x04}));
        SIGNATURES.put("mp4", List.of(new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p'},
                new byte[]{0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p'}));
    }

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private FileStorageUtil() {
        // utility class
    }

    /** Metadata about a stored file - mapped 1:1 to a submission_files row. */
    public static class StoredFile {
        private final String originalName;
        private final String storedName;
        private final String contentType;
        private final long size;
        private final String sha256;

        public StoredFile(String originalName, String storedName, String contentType, long size, String sha256) {
            this.originalName = originalName;
            this.storedName = storedName;
            this.contentType = contentType;
            this.size = size;
            this.sha256 = sha256;
        }

        public String getOriginalName() { return originalName; }
        public String getStoredName() { return storedName; }
        public String getContentType() { return contentType; }
        public long getSize() { return size; }
        public String getSha256() { return sha256; }

        /** Relative path under the upload root, in a stable <year>/<month> subfolder. */
        public String getRelativeLocation() {
            return LocalDateTime.now().format(YEAR_MONTH) + "/" + storedName;
        }
    }

    /** The list shown to the user in the upload form ("accepted files"). */
    public static String acceptedExtensionsForInput() {
        StringBuilder sb = new StringBuilder();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append('.').append(ext);
        }
        return sb.toString();
    }

    public static boolean isAllowedExtension(String filename) {
        return ALLOWED_EXTENSIONS.contains(extensionOf(filename));
    }

    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Validates, stores and hashes an uploaded file.
     *
     * @param part       the multipart part coming from the request
     * @param uploadRoot the configured upload directory (outside the web root)
     * @return metadata ready to be inserted into submission_files
     * @throws ValidationException when the file is empty, too big, or of a forbidden type
     */
    public static StoredFile store(Part part, Path uploadRoot) {
        String submittedName = part.getSubmittedFileName();
        if (submittedName == null || submittedName.trim().isEmpty() || part.getSize() <= 0) {
            throw new ValidationException("The uploaded file is empty.");
        }

        String originalName = sanitiseDisplayName(submittedName);
        String extension = extensionOf(originalName);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("File type '." + extension + "' is not allowed. Allowed types: "
                    + String.join(", ", ALLOWED_EXTENSIONS));
        }

        long max = AppConfig.uploadMaxFileSizeBytes();
        if (part.getSize() > max) {
            throw new ValidationException("File is too large (" + humanReadableSize(part.getSize())
                    + "). Maximum allowed size is " + humanReadableSize(max) + ".");
        }

        // Random storage name: no user input reaches the filesystem path.
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            // Files are grouped by year/month so one folder does not grow forever.
            Path subDir = uploadRoot.resolve(LocalDateTime.now().format(YEAR_MONTH)).normalize();
            if (!subDir.startsWith(uploadRoot)) {
                throw new ValidationException("Invalid upload target directory.");
            }
            Files.createDirectories(subDir);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path target = subDir.resolve(storedName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new ValidationException("Invalid upload target file.");
            }

            try (InputStream rawIn = part.getInputStream();
                 DigestInputStream in = new DigestInputStream(rawIn, digest)) {

                // Read the first bytes for the magic-number check before writing.
                byte[] head = new byte[16];
                int headLen = readFully(in, head);
                if (!signatureMatches(head, headLen, extension)) {
                    throw new ValidationException("The content of '" + originalName
                            + "' does not look like a valid ." + extension + " file. "
                            + "Please upload a genuine file of the allowed types.");
                }

                // Write header bytes, then stream the remainder, enforcing the size cap again.
                try (var out = Files.newOutputStream(target)) {
                    out.write(head, 0, headLen);
                    byte[] buffer = new byte[8192];
                    long written = headLen;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        written += read;
                        if (written > max) {
                            out.close();
                            Files.deleteIfExists(target);
                            throw new ValidationException("File is too large. Maximum allowed size is "
                                    + humanReadableSize(max) + ".");
                        }
                        out.write(buffer, 0, read);
                    }
                }
            }

            return new StoredFile(originalName, storedName, part.getContentType(), part.getSize(),
                    toHex(digest.digest()));

        } catch (IOException e) {
            throw new ValidationException("Could not save the uploaded file: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    /** Deletes a stored file. Never throws - cleanup must not break a request. */
    public static void delete(Path uploadRoot, String relativeLocation) {
        try {
            Path target = uploadRoot.resolve(relativeLocation).normalize();
            if (target.startsWith(uploadRoot)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            System.err.println("[WorkProof] Could not delete evidence file " + relativeLocation + ": " + e.getMessage());
        }
    }

    /** Reads a stored file into a temporary file so it can be streamed to an authorised user. */
    public static Path resolve(Path uploadRoot, String relativeLocation) {
        Path target = uploadRoot.resolve(relativeLocation).normalize();
        if (!target.startsWith(uploadRoot) || !Files.isReadable(target)) {
            return null;
        }
        return target;
    }

    /* ------------------------------------------------------------- helpers */

    /** Removes any path components and control characters from a user-supplied filename. */
    public static String sanitiseDisplayName(String name) {
        String cleaned = name.replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0) {
            cleaned = cleaned.substring(slash + 1);
        }
        cleaned = cleaned.replaceAll("[\\p{Cntrl}]", "").trim();
        if (cleaned.isEmpty()) {
            cleaned = "evidence";
        }
        // Keep the stored display name short so it always fits the DB column.
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private static boolean signatureMatches(byte[] head, int len, String extension) {
        List<byte[]> candidates = SIGNATURES.get(extension);
        if (candidates == null) {
            return true;      // text-like format: extension check is all we can do
        }
        for (byte[] signature : candidates) {
            if (len >= signature.length && startsWith(head, len, signature)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] data, int len, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (i >= len || data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read == -1) {
                break;
            }
            total += read;
        }
        return total;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /** Convenience for tests and callers that only have a byte array (never used for real uploads). */
    public static String sha256Of(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}

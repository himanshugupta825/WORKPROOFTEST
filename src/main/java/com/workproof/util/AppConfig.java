package com.workproof.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Central configuration loader for WorkProof.
 *
 * Resolution order (first value found wins):
 *   1. Environment variables  - e.g. WORKPROOF_DB_PASSWORD            [most secure, used in production]
 *   2. JVM system properties  - e.g. -Dworkproof.db.password=secret    [useful for VS Code launch config]
 *   3. workproof.properties   - classpath file (bundled defaults, NO real password)
 *   4. workproof-local.properties - a LOCAL, .gitignore'd file with your real DB password
 *
 * Nothing in this class ever prints the password, and the class is loaded once,
 * so configuration mistakes surface immediately at application start-up.
 *
 * Example: in Tomcat you can also set these in $CATALINA_BASE/bin/setenv.sh:
 *   export WORKPROOF_DB_URL="jdbc:mysql://localhost:3306/workproof"
 *   export WORKPROOF_DB_USER="workproof_app"
 *   export WORKPROOF_DB_PASSWORD="your-secret"
 */
public final class AppConfig {

    /* ---------- configuration keys ---------- */
    public static final String KEY_DB_URL = "workproof.db.url";
    public static final String KEY_DB_USER = "workproof.db.user";
    public static final String KEY_DB_PASSWORD = "workproof.db.password";
    public static final String KEY_DB_POOL_SIZE = "workproof.db.poolSize";
    public static final String KEY_UPLOAD_DIR = "workproof.upload.dir";
    public static final String KEY_UPLOAD_MAX_MB = "workproof.upload.maxFileSizeMb";
    public static final String KEY_SESSION_TIMEOUT_MIN = "workproof.session.timeoutMinutes";
    public static final String KEY_APP_NAME = "workproof.app.name";
    public static final String KEY_ALLOW_SELF_REVIEW = "workproof.review.allowSelfReview";

    private static final Properties FILE_PROPS = new Properties();
    private static final String CONFIG_FILE = "workproof.properties";
    private static final String LOCAL_CONFIG_FILE = "workproof-local.properties";

    static {
        // 1) bundled defaults (safe, no secrets)
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                FILE_PROPS.load(in);
            }
        } catch (IOException e) {
            System.err.println("[WorkProof] Could not read " + CONFIG_FILE + ": " + e.getMessage());
        }
        // 2) local overrides for the developer machine (ignored by Git)
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(LOCAL_CONFIG_FILE)) {
            if (in != null) {
                FILE_PROPS.load(in);
                System.out.println("[WorkProof] Loaded local configuration from " + LOCAL_CONFIG_FILE);
            }
        } catch (IOException e) {
            System.err.println("[WorkProof] Could not read " + LOCAL_CONFIG_FILE + ": " + e.getMessage());
        }
    }

    private AppConfig() {
        // utility class
    }

    /**
     * Reads a value using the resolution order documented above.
     *
     * @param key          property key, e.g. workproof.db.url
     * @param envVar       matching environment variable name, may be null
     * @param defaultValue value returned when nothing is configured
     */
    public static String get(String key, String envVar, String defaultValue) {
        if (envVar != null) {
            String fromEnv = System.getenv(envVar);
            if (isNotBlank(fromEnv)) {
                return fromEnv.trim();
            }
        }
        String fromSystem = System.getProperty(key);
        if (isNotBlank(fromSystem)) {
            return fromSystem.trim();
        }
        String fromFile = FILE_PROPS.getProperty(key);
        if (isNotBlank(fromFile)) {
            return fromFile.trim();
        }
        return defaultValue;
    }

    /* ---------- typed accessors used by the rest of the application ---------- */

    public static String dbUrl() {
        return get(KEY_DB_URL, "WORKPROOF_DB_URL",
                "jdbc:mysql://localhost:3306/workproof?useSSL=false&allowPublicKeyRetrieval=true"
                        + "&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8&useUnicode=true");
    }

    public static String dbUser() {
        return get(KEY_DB_USER, "WORKPROOF_DB_USER", "root");
    }

    /**
     * The database password. Defaults to an empty string so a fresh checkout
     * does not crash with a confusing error - the app will report a clear
     * connection failure instead.
     */
    public static String dbPassword() {
        return get(KEY_DB_PASSWORD, "WORKPROOF_DB_PASSWORD", "");
    }

    public static int dbPoolSize() {
        return intValue(get(KEY_DB_POOL_SIZE, "WORKPROOF_DB_POOL_SIZE", "10"), 10, 2, 50);
    }

    /** Directory where uploaded evidence is stored (OUTSIDE the web root). */
    public static Path uploadDir() {
        String configured = get(KEY_UPLOAD_DIR, "WORKPROOF_UPLOAD_DIR", null);
        if (isNotBlank(configured)) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        // Sensible default: <user home>/workproof-uploads  (NOT inside the WAR)
        return Paths.get(System.getProperty("user.home"), "workproof-uploads").toAbsolutePath().normalize();
    }

    public static long uploadMaxFileSizeBytes() {
        int mb = intValue(get(KEY_UPLOAD_MAX_MB, "WORKPROOF_UPLOAD_MAX_MB", "10"), 10, 1, 50);
        return mb * 1024L * 1024L;
    }

    public static int sessionTimeoutMinutes() {
        return intValue(get(KEY_SESSION_TIMEOUT_MIN, "WORKPROOF_SESSION_TIMEOUT_MIN", "30"), 30, 5, 480);
    }

    public static String appName() {
        return get(KEY_APP_NAME, "WORKPROOF_APP_NAME", "WorkProof");
    }

    /**
     * Documented project rule: by default a user may NOT review their own submission.
     * Set workproof.review.allowSelfReview=true (or WORKPROOF_ALLOW_SELF_REVIEW=true)
     * only if your institution explicitly allows it.
     */
    public static boolean allowSelfReview() {
        return Boolean.parseBoolean(get(KEY_ALLOW_SELF_REVIEW, "WORKPROOF_ALLOW_SELF_REVIEW", "false"));
    }

    /** Creates the upload directory on first use. Returns true when it is usable. */
    public static boolean ensureUploadDirExists() {
        try {
            Path dir = uploadDir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return Files.isDirectory(dir) && Files.isWritable(dir);
        } catch (IOException e) {
            System.err.println("[WorkProof] Upload directory is not usable: " + e.getMessage());
            return false;
        }
    }

    /* ---------- small helpers ---------- */

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static int intValue(String raw, int fallback, int min, int max) {
        try {
            int v = Integer.parseInt(raw);
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

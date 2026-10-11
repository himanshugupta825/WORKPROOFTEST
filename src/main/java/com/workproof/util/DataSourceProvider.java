package com.workproof.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thin JDBC connection-pool wrapper (the "Database connection utility" of the spec).
 *
 * Why a pool instead of DriverManager.getConnection() everywhere?
 *   - Opening a MySQL connection per query is slow and leaks sockets under Tomcat.
 *   - A pool recycles connections and gives fast failure when MySQL is down.
 *
 * Every DAO calls {@link #getConnection()} and uses try-with-resources, so the
 * connection is always returned to the pool, even when a query throws.
 *
 * Layer rule: ONLY the dao package may call this class.
 */
public final class DataSourceProvider {

    private static volatile HikariDataSource dataSource;
    private static volatile String lastInitError;

    private DataSourceProvider() {
        // utility class
    }

    /** Lazily initialises the pool on first use (thread-safe, double-checked). */
    public static DataSource getDataSource() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (DataSourceProvider.class) {
                local = dataSource;
                if (local == null) {
                    local = createPool();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    private static HikariDataSource createPool() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            lastInitError = "MySQL JDBC driver (com.mysql.cj.jdbc.Driver) was not found on the classpath.";
            throw new IllegalStateException(lastInitError, e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.dbUrl());
        config.setUsername(AppConfig.dbUser());
        // Password comes from env var / system property / local ignored properties file.
        config.setPassword(AppConfig.dbPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("WorkProofPool");
        config.setMaximumPoolSize(AppConfig.dbPoolSize());
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);      // fail fast when MySQL is unreachable
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_200_000);
        config.setAutoCommit(true);               // services switch this off for transactions
        // Prepared-statement cache: real performance + safety win for JDBC.
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        System.out.println("[WorkProof] Initialising connection pool for " + safeUrl(AppConfig.dbUrl())
                + " as user '" + AppConfig.dbUser() + "'");
        return new HikariDataSource(config);
    }

    /** Borrows a connection from the pool. The caller MUST close it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /** Quick health probe used by the start-up listener and the admin dashboard. */
    public static boolean testConnection() {
        try (Connection c = getConnection()) {
            return c.isValid(5);
        } catch (SQLException | RuntimeException e) {
            lastInitError = e.getMessage();
            return false;
        }
    }

    public static String getLastInitError() {
        return lastInitError;
    }

    /** Closes the pool - called by the context listener on application shutdown. */
    public static void shutdown() {
        HikariDataSource local = dataSource;
        if (local != null) {
            local.close();
            dataSource = null;
            System.out.println("[WorkProof] Connection pool closed.");
        }
    }

    /** Removes the password from a JDBC URL before logging it. */
    private static String safeUrl(String url) {
        if (url == null) {
            return "(not configured)";
        }
        return url.replaceAll("(?i)(password=)[^&]*", "$1***");
    }
}

package com.workproof.dao;

import com.workproof.exception.DataAccessException;
import com.workproof.util.DataSourceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all DAOs. It centralises the JDBC plumbing so each DAO only
 * contains SQL and mapping code:
 *
 *   - every query uses a PreparedStatement (SQL-injection protection),
 *   - connections are always closed through try-with-resources (no leaks),
 *   - SQL exceptions are wrapped in DataAccessException with a safe message,
 *   - multi-statement operations can run inside one transaction via {@link #tx}.
 */
public abstract class BaseDao {

    /** Maps the current row of a ResultSet to a domain object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** A unit of work that runs with an open, transactional connection. */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /* ============================ query helpers ============================ */

    protected Connection openConnection() throws SQLException {
        return DataSourceProvider.getConnection();
    }

    protected <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = openConnection()) {
            return list(c, sql, mapper, params);
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + shortSql(sql), e);
        }
    }

    protected <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        }
    }

    /** Returns the first row, or null when the query matched nothing. */
    protected <T> T one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = list(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    protected <T> T one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> rows = list(c, sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** INSERT / UPDATE / DELETE. Returns the number of affected rows. */
    protected int update(String sql, Object... params) {
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + shortSql(sql), e);
        }
    }

    protected int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT that returns the generated AUTO_INCREMENT id. */
    protected int insert(String sql, Object... params) {
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + shortSql(sql), e);
        }
    }

    protected int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** Convenience COUNT query. Never returns null. */
    protected int count(String sql, Object... params) {
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count failed: " + shortSql(sql), e);
        }
    }

    protected int count(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /* ============================ transactions ============================ */

    /**
     * Runs several statements as one atomic unit: they either all succeed or all
     * roll back. Used for example when creating a project together with its owner
     * membership, or a submission together with its uploaded files.
     */
    protected <T> T tx(TransactionWork<T> work) {
        try (Connection c = openConnection()) {
            boolean previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException rollbackFailure) {
                    System.err.println("[WorkProof] Rollback failed: " + rollbackFailure.getMessage());
                }
                throw e instanceof RuntimeException
                        ? (RuntimeException) e
                        : new DataAccessException("Transaction failed", e);
            } finally {
                c.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not open a database transaction", e);
        }
    }

    /* ============================== binding =============================== */

    /**
     * Binds parameters positionally and converts Java 8 date types to JDBC types.
     * Using setObject with the right mapping avoids MySQL timezone surprises.
     */
    protected void bind(PreparedStatement ps, Object[] params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;
            if (value == null) {
                ps.setObject(index, null);
            } else if (value instanceof LocalDate) {
                ps.setDate(index, java.sql.Date.valueOf((LocalDate) value));
            } else if (value instanceof LocalDateTime) {
                ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
            } else if (value instanceof Boolean) {
                ps.setBoolean(index, (Boolean) value);
            } else if (value instanceof Enum) {
                ps.setString(index, ((Enum<?>) value).name());
            } else {
                ps.setObject(index, value);
            }
        }
    }

    /* ============================== helpers =============================== */

    protected static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    protected static LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }

    protected static Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    /** Keeps log messages short so a long SQL string never floods the console. */
    private static String shortSql(String sql) {
        String compact = sql.replaceAll("\\s+", " ").trim();
        return compact.length() > 90 ? compact.substring(0, 90) + "..." : compact;
    }
}

package com.workproof.dao;

import com.workproof.model.ActivityLog;
import com.workproof.model.PagedResult;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data access for `activity_logs` (the audit trail). */
public class ActivityLogDao extends BaseDao {

    private static final String BASE_SELECT =
            "SELECT a.id, a.user_id, a.action, a.entity_type, a.entity_id, a.details, a.ip_address, "
          + "       a.created_at, u.full_name AS user_name "
          + "FROM activity_logs a LEFT JOIN users u ON u.id = a.user_id ";

    private final RowMapper<ActivityLog> mapper = (ResultSet rs) -> {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : userId);
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        int entityId = rs.getInt("entity_id");
        log.setEntityId(rs.wasNull() ? null : entityId);
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        log.setUserName(rs.getString("user_name"));
        return log;
    };

    /* ================================ reads ================================ */

    public List<ActivityLog> findRecent(int limit) {
        return list(BASE_SELECT + "ORDER BY a.created_at DESC LIMIT ?", mapper, limit);
    }

    /** Activity of one user - powers the "my work history" section of the profile page. */
    public List<ActivityLog> findRecentForUser(int userId, int limit) {
        return list(BASE_SELECT + "WHERE a.user_id = ? ORDER BY a.created_at DESC LIMIT ?",
                mapper, userId, limit);
    }

    /** Activity of everybody who works on a project. */
    public List<ActivityLog> findRecentForProject(int projectId, int limit) {
        return list(BASE_SELECT + "WHERE (a.entity_type = 'PROJECT' AND a.entity_id = ?) "
                       + "   OR (a.entity_type = 'TASK' AND a.entity_id IN "
                       + "       (SELECT id FROM tasks WHERE project_id = ?)) "
                       + "   OR (a.entity_type = 'SUBMISSION' AND a.entity_id IN "
                       + "       (SELECT s.id FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
                       + "        WHERE t.project_id = ?)) "
                       + "ORDER BY a.created_at DESC LIMIT ?",
                mapper, projectId, projectId, projectId, limit);
    }

    /** History of one entity, e.g. all log entries of a single task. */
    public List<ActivityLog> findRecentForEntity(String entityType, int entityId, int limit) {
        return list(BASE_SELECT + "WHERE a.entity_type = ? AND a.entity_id = ? "
                + "ORDER BY a.created_at DESC LIMIT ?", mapper, entityType, entityId, limit);
    }

    public PagedResult<ActivityLog> search(String search, String action, Integer userId,
                                           LocalDate dateFrom, LocalDate dateTo, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (a.details LIKE ? OR a.action LIKE ? OR u.full_name LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (action != null && !action.trim().isEmpty() && !"ALL".equalsIgnoreCase(action)) {
            where.append(" AND a.action = ? ");
            params.add(action.trim());
        }
        if (userId != null) {
            where.append(" AND a.user_id = ? ");
            params.add(userId);
        }
        if (dateFrom != null) {
            where.append(" AND a.created_at >= ? ");
            params.add(dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            where.append(" AND a.created_at < ? ");
            params.add(dateTo.plusDays(1).atStartOfDay());
        }

        int total = count("SELECT COUNT(*) FROM activity_logs a LEFT JOIN users u ON u.id = a.user_id " + where,
                params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<ActivityLog> items = list(BASE_SELECT + where + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        return new PagedResult<>(items, total, page, pageSize);
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM activity_logs");
    }

    public int countForUser(int userId) {
        return count("SELECT COUNT(*) FROM activity_logs WHERE user_id = ?", userId);
    }

    /* ================================ write ================================ */

    /**
     * Appends an audit entry. Logging must never break the main operation, so this
     * method swallows database errors after printing them.
     */
    public void log(Integer userId, String action, String entityType, Integer entityId,
                    String details, String ipAddress) {
        try {
            update("INSERT INTO activity_logs (user_id, action, entity_type, entity_id, details, ip_address) "
                 + "VALUES (?, ?, ?, ?, ?, ?)",
                    userId, action, entityType, entityId, truncate(details, 500), truncate(ipAddress, 45));
        } catch (RuntimeException e) {
            System.err.println("[WorkProof] Could not write activity log for action " + action + ": " + e.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}

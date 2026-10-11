package com.workproof.dao;

import com.workproof.model.ReportRow;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All queries behind the Reports module. Each method returns a list of
 * {@link ReportRow} so the JSP pages can share one table-rendering fragment.
 *
 * Every report supports the same optional filters:
 *   dateFrom / dateTo (inclusive), projectId, and - where it makes sense - a user id.
 */
public class ReportDao extends BaseDao {

    private final RowMapper<ReportRow> mapper = (ResultSet rs) -> {
        ReportRow row = new ReportRow();
        row.setId(rs.getInt("id"));
        row.setPrimaryLabel(rs.getString("primary_label"));
        row.setSecondaryLabel(getStringSafe(rs, "secondary_label"));
        row.setCategory(getStringSafe(rs, "category"));
        row.setOwner(getStringSafe(rs, "owner"));
        row.setDate(toLocalDate(getDateSafe(rs, "ref_date")));
        row.setTotalTasks(getIntSafe(rs, "total_tasks"));
        row.setCompletedTasks(getIntSafe(rs, "completed_tasks"));
        row.setApprovedTasks(getIntSafe(rs, "approved_tasks"));
        row.setRejectedTasks(getIntSafe(rs, "rejected_tasks"));
        row.setPendingTasks(getIntSafe(rs, "pending_tasks"));
        row.setOverdueTasks(getIntSafe(rs, "overdue_tasks"));
        row.setOpenTasks(getIntSafe(rs, "open_tasks"));
        row.setProgressPercent(getDoubleSafe(rs, "progress_percent"));
        row.setNote(getStringSafe(rs, "note"));
        return row;
    };

    /* ---------------------------------------------------------------- helpers */

    private static String getStringSafe(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return null;
        }
    }

    private static int getIntSafe(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double getDoubleSafe(ResultSet rs, String column) {
        try {
            return rs.getDouble(column);
        } catch (Exception e) {
            return 0;
        }
    }

    private static java.sql.Date getDateSafe(ResultSet rs, String column) {
        try {
            return rs.getDate(column);
        } catch (Exception e) {
            return null;
        }
    }

    /* ======================= 1. project progress report ======================= */

    /**
     * Progress of every project the viewer is allowed to see.
     *
     * @param viewerId when not null, only projects the user is a member of
     * @param status   project status filter ("ALL" or null = no filter)
     */
    public List<ReportRow> projectProgress(Integer viewerId, String status, LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.name AS primary_label, p.code AS secondary_label, p.status AS category, "
              + "       u.full_name AS owner, p.end_date AS ref_date, "
              + "       COUNT(t.id) AS total_tasks, "
              + "       SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_tasks, "
              + "       SUM(CASE WHEN t.status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_tasks, "
              + "       SUM(CASE WHEN t.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_tasks, "
              + "       SUM(CASE WHEN t.status IN ('SUBMITTED','IN_REVIEW') THEN 1 ELSE 0 END) AS pending_tasks, "
              + "       SUM(CASE WHEN t.status <> 'COMPLETED' AND t.due_date < CURDATE() THEN 1 ELSE 0 END) AS overdue_tasks, "
              + "       NULL AS open_tasks, "
              + "       ROUND(100.0 * SUM(CASE WHEN t.status IN ('APPROVED','COMPLETED') THEN 1 ELSE 0 END) "
              + "             / NULLIF(COUNT(t.id), 0), 1) AS progress_percent, "
              + "       CONCAT(p.start_date, ' to ', p.end_date) AS note "
              + "FROM projects p JOIN users u ON u.id = p.created_by LEFT JOIN tasks t ON t.project_id = p.id ");

        if (viewerId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = p.id AND pm.user_id = ?) ");
            params.add(viewerId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND p.status = ? ");
            params.add(status);
        }
        // Date filter applies to the project window (overlap with the selected range).
        if (from != null) {
            where.append(" AND p.end_date >= ? ");
            params.add(from);
        }
        if (to != null) {
            where.append(" AND p.start_date <= ? ");
            params.add(to);
        }

        sql.append(where)
           .append(" GROUP BY p.id, p.name, p.code, p.status, u.full_name, p.start_date, p.end_date, p.end_date ")
           .append(" ORDER BY progress_percent DESC, p.name ASC");

        return list(sql.toString(), mapper, params.toArray());
    }

    /* ======================== 2. task completion report ======================== */

    public List<ReportRow> taskCompletion(Integer viewerId, Integer projectId, String taskStatus,
                                          LocalDate from, LocalDate to) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");

        if (viewerId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = t.project_id "
                       + "             AND pm.user_id = ?) ");
            params.add(viewerId);
        }
        if (projectId != null) {
            where.append(" AND t.project_id = ? ");
            params.add(projectId);
        }
        if (taskStatus != null && !taskStatus.isEmpty() && !"ALL".equalsIgnoreCase(taskStatus)) {
            where.append(" AND t.status = ? ");
            params.add(taskStatus);
        }
        if (from != null) {
            where.append(" AND t.due_date >= ? ");
            params.add(from);
        }
        if (to != null) {
            where.append(" AND t.due_date <= ? ");
            params.add(to);
        }

        String sql = "SELECT t.id, t.title AS primary_label, p.name AS secondary_label, t.status AS category, "
                   + "       COALESCE(GROUP_CONCAT(DISTINCT u.full_name ORDER BY u.full_name SEPARATOR ', '), 'Unassigned') AS owner, "
                   + "       t.due_date AS ref_date, "
                   + "       1 AS total_tasks, "
                   + "       CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END AS completed_tasks, "
                   + "       CASE WHEN t.status = 'APPROVED' THEN 1 ELSE 0 END AS approved_tasks, "
                   + "       CASE WHEN t.status = 'REJECTED' THEN 1 ELSE 0 END AS rejected_tasks, "
                   + "       CASE WHEN t.status IN ('SUBMITTED','IN_REVIEW') THEN 1 ELSE 0 END AS pending_tasks, "
                   + "       CASE WHEN t.status NOT IN ('COMPLETED') AND t.due_date < CURDATE() THEN 1 ELSE 0 END AS overdue_tasks, "
                   + "       NULL AS open_tasks, "
                   + "       CASE t.status WHEN 'COMPLETED' THEN 100 WHEN 'APPROVED' THEN 90 WHEN 'IN_REVIEW' THEN 70 "
                   + "                     WHEN 'SUBMITTED' THEN 60 WHEN 'IN_PROGRESS' THEN 35 ELSE 0 END AS progress_percent, "
                   + "       CONCAT('Priority: ', t.priority) AS note "
                   + "FROM tasks t "
                   + "JOIN projects p ON p.id = t.project_id "
                   + "LEFT JOIN task_assignments ta ON ta.task_id = t.id "
                   + "LEFT JOIN users u ON u.id = ta.user_id "
                   + where
                   + " GROUP BY t.id, t.title, p.name, t.status, t.due_date, t.priority "
                   + " ORDER BY t.due_date ASC";

        return list(sql, mapper, params.toArray());
    }

    /* ==================== 3. individual contribution report ==================== */

    public List<ReportRow> contributionSummary(Integer viewerId, Integer projectId, Integer userId,
                                               LocalDate from, LocalDate to) {
        // Values for the outer WHERE clause (collected here, bound after the sub-query ones).
        List<Object> whereParams = new ArrayList<>();

        StringBuilder where = new StringBuilder(" WHERE u.status = 'ACTIVE' AND r.name <> 'ADMIN' ");

        if (projectId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = ? AND pm.user_id = u.id) ");
            whereParams.add(projectId);
        }
        if (viewerId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm2 JOIN project_members pm3 "
                       + "             ON pm3.project_id = pm2.project_id "
                       + "             WHERE pm2.user_id = ? AND pm3.user_id = u.id) ");
            whereParams.add(viewerId);
        }
        if (userId != null) {
            where.append(" AND u.id = ? ");
            whereParams.add(userId);
        }

        // Date window used inside the assignment sub-queries.
        String taskDateWindow = dateRange("ta.assigned_at", from, to);

        String sql = "SELECT u.id, u.full_name AS primary_label, r.name AS secondary_label, "
                   + "       r.name AS category, u.department AS owner, u.created_at AS ref_date, "
                   + "       (SELECT COUNT(*) FROM task_assignments ta JOIN tasks t2 ON t2.id = ta.task_id "
                   + "          WHERE ta.user_id = u.id " + taskDateWindow + ") AS total_tasks, "
                   + "       (SELECT COUNT(*) FROM task_assignments ta JOIN tasks t2 ON t2.id = ta.task_id "
                   + "          WHERE ta.user_id = u.id AND t2.status = 'COMPLETED' " + taskDateWindow + ") AS completed_tasks, "
                   + "       (SELECT COUNT(*) FROM proof_submissions s WHERE s.submitted_by = u.id AND s.status = 'APPROVED') AS approved_tasks, "
                   + "       (SELECT COUNT(*) FROM proof_submissions s WHERE s.submitted_by = u.id AND s.status = 'REJECTED') AS rejected_tasks, "
                   + "       (SELECT COUNT(*) FROM proof_submissions s WHERE s.submitted_by = u.id AND s.status = 'PENDING') AS pending_tasks, "
                   + "       (SELECT COUNT(*) FROM task_assignments ta JOIN tasks t2 ON t2.id = ta.task_id "
                   + "          WHERE ta.user_id = u.id AND t2.status <> 'COMPLETED' AND t2.due_date < CURDATE()) AS overdue_tasks, "
                   + "       (SELECT COUNT(*) FROM task_assignments ta JOIN tasks t2 ON t2.id = ta.task_id "
                   + "          WHERE ta.user_id = u.id AND t2.status IN ('TODO','IN_PROGRESS')) AS open_tasks, "
                   + "       ROUND(100.0 * (SELECT COUNT(*) FROM task_assignments ta JOIN tasks t2 ON t2.id = ta.task_id "
                   + "                     WHERE ta.user_id = u.id AND t2.status = 'COMPLETED') "
                   + "             / NULLIF((SELECT COUNT(*) FROM task_assignments ta JOIN tasks t2 ON t2.id = ta.task_id "
                   + "                        WHERE ta.user_id = u.id), 0), 1) AS progress_percent, "
                   + "       COALESCE(u.designation, u.department, 'Team member') AS note "
                   + "FROM users u JOIN roles r ON r.id = u.role_id " + where
                   + " ORDER BY completed_tasks DESC, u.full_name ASC";

        // Bind in SQL text order: the sub-query placeholders (in the SELECT list) come
        // first, so their values are added once per occurrence, then the WHERE clause.
        List<Object> dateValues = dateRangeValues(from, to);
        List<Object> subQueryParams = new ArrayList<>();
        for (int i = 0; i < occurrences(sql, taskDateWindow); i++) {
            subQueryParams.addAll(dateValues);
        }
        List<Object> allParams = new ArrayList<>(subQueryParams);
        allParams.addAll(whereParams);
        return list(sql, mapper, allParams.toArray());
    }

    /**
     * Builds an optional date window such as " AND ta.assigned_at >= ? AND ta.assigned_at <= ? ".
     * The dates are ALWAYS bound as parameters (never concatenated text), so user
     * supplied dates cannot alter the query.
     */
    private String dateRange(String column, LocalDate from, LocalDate to) {
        StringBuilder condition = new StringBuilder();
        if (from != null) {
            condition.append(" AND ").append(column).append(" >= ? ");
        }
        if (to != null) {
            condition.append(" AND ").append(column).append(" <= ? ");
        }
        return condition.toString();
    }

    /** Values that must be bound for every {@link #dateRange} fragment. */
    private List<Object> dateRangeValues(LocalDate from, LocalDate to) {
        List<Object> values = new ArrayList<>();
        if (from != null) {
            values.add(from);
        }
        if (to != null) {
            values.add(to);
        }
        return values;
    }

    /** Number of times a fragment occurs in the final SQL text (used for parameter order). */
    private static int occurrences(String text, String fragment) {
        if (fragment.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = text.indexOf(fragment);
        while (index >= 0) {
            count++;
            index = text.indexOf(fragment, index + fragment.length());
        }
        return count;
    }

    /* ================= 4. pending verification report ================= */

    public List<ReportRow> pendingVerification(Integer viewerId, Integer projectId, LocalDate from, LocalDate to) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE s.status = 'PENDING' ");

        if (viewerId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = t.project_id "
                       + "             AND pm.user_id = ? AND pm.role_in_project IN ('REVIEWER','MANAGER')) ");
            params.add(viewerId);
        }
        if (projectId != null) {
            where.append(" AND t.project_id = ? ");
            params.add(projectId);
        }
        if (from != null) {
            where.append(" AND s.submitted_at >= ? ");
            params.add(from.atStartOfDay());
        }
        if (to != null) {
            where.append(" AND s.submitted_at < ? ");
            params.add(to.plusDays(1).atStartOfDay());
        }

        String sql = "SELECT s.id, s.title AS primary_label, t.title AS secondary_label, s.status AS category, "
                   + "       ub.full_name AS owner, s.submitted_at AS ref_date, "
                   + "       NULL AS total_tasks, NULL AS completed_tasks, NULL AS approved_tasks, "
                   + "       NULL AS rejected_tasks, NULL AS pending_tasks, NULL AS overdue_tasks, "
                   + "       DATEDIFF(NOW(), s.submitted_at) AS open_tasks, "
                   + "       NULL AS progress_percent, "
                   + "       CONCAT('Waiting ', DATEDIFF(NOW(), s.submitted_at), ' day(s) · v', s.version, "
                   + "              ' · ', (SELECT COUNT(*) FROM submission_files f WHERE f.submission_id = s.id), ' file(s)') AS note "
                   + "FROM proof_submissions s "
                   + "JOIN tasks t ON t.id = s.task_id "
                   + "JOIN projects p ON p.id = t.project_id "
                   + "JOIN users ub ON ub.id = s.submitted_by "
                   + where
                   + " ORDER BY s.submitted_at ASC";

        return list(sql, mapper, params.toArray());
    }

    /* ==================== 5. rejected submissions report ==================== */

    public List<ReportRow> rejectedSubmissions(Integer viewerId, Integer projectId, Integer userId,
                                               LocalDate from, LocalDate to) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE s.status = 'REJECTED' ");

        if (viewerId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = t.project_id "
                       + "             AND pm.user_id = ?) ");
            params.add(viewerId);
        }
        if (projectId != null) {
            where.append(" AND t.project_id = ? ");
            params.add(projectId);
        }
        if (userId != null) {
            where.append(" AND s.submitted_by = ? ");
            params.add(userId);
        }
        if (from != null) {
            where.append(" AND s.submitted_at >= ? ");
            params.add(from.atStartOfDay());
        }
        if (to != null) {
            where.append(" AND s.submitted_at < ? ");
            params.add(to.plusDays(1).atStartOfDay());
        }

        String sql = "SELECT s.id, s.title AS primary_label, t.title AS secondary_label, s.status AS category, "
                   + "       CONCAT(ub.full_name, ' → reviewed by ', COALESCE(ur.full_name, 'n/a')) AS owner, "
                   + "       s.submitted_at AS ref_date, "
                   + "       NULL AS total_tasks, NULL AS completed_tasks, NULL AS approved_tasks, "
                   + "       NULL AS rejected_tasks, NULL AS pending_tasks, NULL AS overdue_tasks, "
                   + "       s.version AS open_tasks, NULL AS progress_percent, "
                   + "       COALESCE(r.comments, 'No comment recorded') AS note "
                   + "FROM proof_submissions s "
                   + "JOIN tasks t ON t.id = s.task_id "
                   + "JOIN projects p ON p.id = t.project_id "
                   + "JOIN users ub ON ub.id = s.submitted_by "
                   + "LEFT JOIN reviews r ON r.submission_id = s.id "
                   + "LEFT JOIN users ur ON ur.id = r.reviewer_id "
                   + where
                   + " ORDER BY s.submitted_at DESC";

        return list(sql, mapper, params.toArray());
    }
}

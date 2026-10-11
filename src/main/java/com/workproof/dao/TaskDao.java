package com.workproof.dao;

import com.workproof.model.PagedResult;
import com.workproof.model.ProjectMember;
import com.workproof.model.Task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for `tasks` and `task_assignments`.
 *
 * Sorting is whitelisted through {@link #orderByClause(String, String)} - a user
 * supplied sort key is never concatenated into SQL directly.
 */
public class TaskDao extends BaseDao {

    private static final String BASE_SELECT =
            "SELECT t.id, t.project_id, t.title, t.description, t.priority, t.status, t.start_date, "
          + "       t.due_date, t.created_by, t.completed_at, t.created_at, t.updated_at, "
          + "       p.name AS project_name, p.code AS project_code, u.full_name AS creator_name "
          + "FROM tasks t "
          + "JOIN projects p ON p.id = t.project_id "
          + "JOIN users u ON u.id = t.created_by ";

    private final RowMapper<Task> mapper = (ResultSet rs) -> {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setProjectId(rs.getInt("project_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setPriority(rs.getString("priority"));
        t.setStatus(rs.getString("status"));
        t.setStartDate(toLocalDate(rs.getDate("start_date")));
        t.setDueDate(toLocalDate(rs.getDate("due_date")));
        t.setCreatedBy(rs.getInt("created_by"));
        t.setCompletedAt(toLocalDateTime(rs.getTimestamp("completed_at")));
        t.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        t.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        t.setProjectName(rs.getString("project_name"));
        t.setProjectCode(rs.getString("project_code"));
        t.setCreatorName(rs.getString("creator_name"));
        return t;
    };

    private final RowMapper<ProjectMember> assigneeMapper = (ResultSet rs) -> {
        ProjectMember m = new ProjectMember();
        m.setUserId(rs.getInt("user_id"));
        m.setFullName(rs.getString("full_name"));
        m.setEmail(rs.getString("email"));
        return m;
    };

    private static final String ASSIGNEE_SELECT =
            "SELECT ta.task_id, ta.user_id, u.full_name, u.email "
          + "FROM task_assignments ta JOIN users u ON u.id = ta.user_id ";

    /* ================================ reads ================================ */

    public Task findById(int id) {
        Task task = one(BASE_SELECT + "WHERE t.id = ?", mapper, id);
        if (task != null) {
            enrichTasks(List.of(task));
        }
        return task;
    }

    public List<Task> findByProject(int projectId) {
        List<Task> tasks = list(BASE_SELECT + "WHERE t.project_id = ? ORDER BY "
                + "FIELD(t.status, 'REJECTED','IN_REVIEW','SUBMITTED','IN_PROGRESS','TODO','APPROVED','COMPLETED'), "
                + "t.due_date ASC", mapper, projectId);
        enrichTasks(tasks);
        return tasks;
    }

    /**
     * Filtered, paged task search.
     *
     * @param assigneeUserId when set, only tasks assigned to that user are returned
     * @param projectId      optional project filter
     * @param status         exact status or null/"ALL"
     * @param priority       exact priority or null/"ALL"
     * @param search         free text over title and description
     * @param sortKey        one of due, created, title, priority, status (whitelisted)
     */
    public PagedResult<Task> search(Integer assigneeUserId, Integer projectId, String status, String priority,
                                    String search, String sortKey, String sortDir, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (assigneeUserId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM task_assignments ta "
                       + "             WHERE ta.task_id = t.id AND ta.user_id = ?) ");
            params.add(assigneeUserId);
        }
        if (projectId != null) {
            where.append(" AND t.project_id = ? ");
            params.add(projectId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND t.status = ? ");
            params.add(status);
        }
        if (priority != null && !priority.isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            where.append(" AND t.priority = ? ");
            params.add(priority);
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (t.title LIKE ? OR t.description LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }

        // count query needs the joins because the WHERE clause references t/p/u aliases
        String countSql = "SELECT COUNT(*) FROM tasks t JOIN projects p ON p.id = t.project_id "
                        + "JOIN users u ON u.id = t.created_by " + where;
        int total = count(countSql, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        String sql = BASE_SELECT + where + orderByClause(sortKey, sortDir) + " LIMIT ? OFFSET ?";
        List<Task> tasks = list(sql, mapper, pageParams.toArray());
        enrichTasks(tasks);
        return new PagedResult<>(tasks, total, page, pageSize);
    }

    /**
     * Same filters as {@link #search} but restricted to an explicit list of projects.
     * This is how a team member is kept inside their own projects.
     */
    public PagedResult<Task> searchInProjects(Integer assigneeUserId, List<Integer> projectIds, String status,
                                              String priority, String search, String sortKey, String sortDir,
                                              int page, int pageSize) {
        if (projectIds == null || projectIds.isEmpty()) {
            return PagedResult.empty(pageSize);
        }
        String placeholders = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        StringBuilder where = new StringBuilder(" WHERE t.project_id IN (" + placeholders + ") ");
        List<Object> params = new ArrayList<>(projectIds);

        if (assigneeUserId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM task_assignments ta "
                       + "             WHERE ta.task_id = t.id AND ta.user_id = ?) ");
            params.add(assigneeUserId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND t.status = ? ");
            params.add(status);
        }
        if (priority != null && !priority.isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            where.append(" AND t.priority = ? ");
            params.add(priority);
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (t.title LIKE ? OR t.description LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }

        int total = count("SELECT COUNT(*) FROM tasks t JOIN projects p ON p.id = t.project_id "
                        + "JOIN users u ON u.id = t.created_by " + where, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<Task> tasks = list(BASE_SELECT + where + orderByClause(sortKey, sortDir) + " LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        enrichTasks(tasks);
        return new PagedResult<>(tasks, total, page, pageSize);
    }

    /** Tasks assigned to a user that are still open - used on dashboards and the member home page. */
    public List<Task> findOpenForUser(int userId, int limit) {
        List<Task> tasks = list(BASE_SELECT
                + "JOIN task_assignments ta ON ta.task_id = t.id AND ta.user_id = ? "
                + "WHERE t.status NOT IN ('COMPLETED') "
                + "ORDER BY t.due_date ASC LIMIT ?", mapper, userId, limit);
        enrichTasks(tasks);
        return tasks;
    }

    /** Upcoming / overdue deadlines for a user (dashboard widget). */
    public List<Task> findUpcomingDeadlinesForUser(int userId, int limit) {
        List<Task> tasks = list(BASE_SELECT
                + "JOIN task_assignments ta ON ta.task_id = t.id AND ta.user_id = ? "
                + "WHERE t.status NOT IN ('COMPLETED') AND t.due_date <= DATE_ADD(CURDATE(), INTERVAL 14 DAY) "
                + "ORDER BY t.due_date ASC LIMIT ?", mapper, userId, limit);
        enrichTasks(tasks);
        return tasks;
    }

    /** Upcoming deadlines for a whole project. */
    public List<Task> findUpcomingDeadlinesForProject(int projectId, int limit) {
        List<Task> tasks = list(BASE_SELECT
                + "WHERE t.project_id = ? AND t.status NOT IN ('COMPLETED') "
                + "ORDER BY t.due_date ASC LIMIT ?", mapper, projectId, limit);
        enrichTasks(tasks);
        return tasks;
    }

    /** Project-wide upcoming deadlines for managers/reviewers/admins. */
    public List<Task> findUpcomingDeadlinesAll(int limit) {
        List<Task> tasks = list(BASE_SELECT
                + "WHERE t.status NOT IN ('COMPLETED') AND t.due_date <= DATE_ADD(CURDATE(), INTERVAL 14 DAY) "
                + "ORDER BY t.due_date ASC LIMIT ?", mapper, limit);
        enrichTasks(tasks);
        return tasks;
    }

    /** Used by the scheduled deadline job: open tasks due within N days. */
    public List<Task> findDueWithinDays(int days) {
        List<Task> tasks = list(BASE_SELECT
                + "WHERE t.status NOT IN ('COMPLETED', 'APPROVED') "
                + "  AND t.due_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY) "
                + "ORDER BY t.due_date", mapper, days);
        enrichTasks(tasks);
        return tasks;
    }

    public List<Task> findOverdueTasks() {
        List<Task> tasks = list(BASE_SELECT
                + "WHERE t.status NOT IN ('COMPLETED', 'APPROVED') AND t.due_date < CURDATE() "
                + "ORDER BY t.due_date", mapper);
        enrichTasks(tasks);
        return tasks;
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM tasks");
    }

    public int countByStatus(String status) {
        return count("SELECT COUNT(*) FROM tasks WHERE status = ?", status);
    }

    public int countInStatuses(String... statuses) {
        if (statuses == null || statuses.length == 0) {
            return 0;
        }
        String placeholders = String.join(",", Collections.nCopies(statuses.length, "?"));
        return count("SELECT COUNT(*) FROM tasks WHERE status IN (" + placeholders + ")", (Object[]) statuses);
    }

    public int countForAssigneeInStatuses(int userId, String... statuses) {
        String placeholders = String.join(",", Collections.nCopies(statuses.length, "?"));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        Collections.addAll(params, statuses);
        return count("SELECT COUNT(*) FROM task_assignments ta JOIN tasks t ON t.id = ta.task_id "
                   + "WHERE ta.user_id = ? AND t.status IN (" + placeholders + ")", params.toArray());
    }

    public int countForAssignee(int userId) {
        return count("SELECT COUNT(*) FROM task_assignments WHERE user_id = ?", userId);
    }

    public int countForAssignee(int userId, int projectId) {
        return count("SELECT COUNT(*) FROM task_assignments ta JOIN tasks t ON t.id = ta.task_id "
                   + "WHERE ta.user_id = ? AND t.project_id = ?", userId, projectId);
    }

    public int countForUserProjectScope(int userId) {
        return count("SELECT COUNT(DISTINCT t.id) FROM tasks t "
                   + "JOIN project_members pm ON pm.project_id = t.project_id WHERE pm.user_id = ?", userId);
    }

    /** Per-status counts for the dashboard doughnut chart. */
    public Map<String, Integer> countGroupedByStatusForUser(int userId, boolean onlyAssigned) {
        String sql = onlyAssigned
                ? "SELECT t.status, COUNT(*) AS total FROM tasks t "
                + "JOIN task_assignments ta ON ta.task_id = t.id WHERE ta.user_id = ? GROUP BY t.status"
                : "SELECT t.status, COUNT(*) AS total FROM tasks t "
                + "JOIN project_members pm ON pm.project_id = t.project_id WHERE pm.user_id = ? GROUP BY t.status";
        Map<String, Integer> result = new HashMap<>();
        list(sql, (ResultSet rs) -> {
            result.put(rs.getString("status"), rs.getInt("total"));
            return null;
        }, userId);
        return result;
    }

    /** Same as above but across the whole system (admin / manager views). */
    public Map<String, Integer> countGroupedByStatusAll() {
        Map<String, Integer> result = new HashMap<>();
        list("SELECT status, COUNT(*) AS total FROM tasks GROUP BY status", (ResultSet rs) -> {
            result.put(rs.getString("status"), rs.getInt("total"));
            return null;
        });
        return result;
    }

    /**
     * Counts tasks in one status inside a scope condition.
     * The scope fragment is a constant defined by DashboardDao (never user input)
     * and contains exactly one "?" placeholder when userId is not null.
     */
    public int countForScope(String scopeCondition, Integer userId, String status) {
        String sql = "SELECT COUNT(*) FROM tasks t WHERE t.status = ? " + scopeCondition;
        return userId == null ? count(sql, status) : count(sql, status, userId);
    }

    /* ================================ writes ================================ */

    /**
     * Creates a task and its assignees inside ONE transaction, so a task is never
     * left without owners when an insert fails halfway.
     */
    public int createWithAssignees(final Task task, final List<Integer> assigneeIds, final int assignedBy) {
        return tx((Connection c) -> {
            int taskId = insert(c, "INSERT INTO tasks (project_id, title, description, priority, status, "
                                 + "start_date, due_date, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    task.getProjectId(), task.getTitle(), task.getDescription(), task.getPriority(),
                    task.getStatus(), task.getStartDate(), task.getDueDate(), task.getCreatedBy());
            if (assigneeIds != null) {
                for (Integer userId : assigneeIds) {
                    if (userId == null) {
                        continue;
                    }
                    insert(c, "INSERT INTO task_assignments (task_id, user_id, assigned_by) VALUES (?, ?, ?)",
                            taskId, userId, assignedBy);
                }
            }
            return taskId;
        });
    }

    public int update(Task task) {
        return update("UPDATE tasks SET title = ?, description = ?, priority = ?, start_date = ?, due_date = ? "
                    + "WHERE id = ?",
                task.getTitle(), task.getDescription(), task.getPriority(), task.getStartDate(),
                task.getDueDate(), task.getId());
    }

    /** Status change with the completion timestamp maintained automatically. */
    public int updateStatus(int taskId, String status) {
        if ("COMPLETED".equals(status)) {
            return update("UPDATE tasks SET status = ?, completed_at = ? WHERE id = ?",
                    status, LocalDateTime.now(), taskId);
        }
        return update("UPDATE tasks SET status = ?, completed_at = NULL WHERE id = ?", status, taskId);
    }

    public int updateStatus(Connection c, int taskId, String status) throws java.sql.SQLException {
        if ("COMPLETED".equals(status)) {
            return update(c, "UPDATE tasks SET status = ?, completed_at = ? WHERE id = ?",
                    status, LocalDateTime.now(), taskId);
        }
        return update(c, "UPDATE tasks SET status = ?, completed_at = NULL WHERE id = ?", status, taskId);
    }

    public int delete(int taskId) {
        return update("DELETE FROM tasks WHERE id = ?", taskId);
    }

    /* ============================ task assignees ============================ */

    public List<ProjectMember> findAssignees(int taskId) {
        return list(ASSIGNEE_SELECT + "WHERE ta.task_id = ? ORDER BY u.full_name", assigneeMapper, taskId);
    }

    public List<Integer> findAssigneeIds(int taskId) {
        List<Integer> ids = new ArrayList<>();
        list("SELECT user_id FROM task_assignments WHERE task_id = ?",
                (ResultSet rs) -> { ids.add(rs.getInt(1)); return null; }, taskId);
        return ids;
    }

    public boolean isAssignedTo(int taskId, int userId) {
        return count("SELECT COUNT(*) FROM task_assignments WHERE task_id = ? AND user_id = ?", taskId, userId) > 0;
    }

    /** Replaces the assignee list of a task atomically. */
    public void replaceAssignees(final int taskId, final List<Integer> userIds, final int assignedBy) {
        tx((Connection c) -> {
            update(c, "DELETE FROM task_assignments WHERE task_id = ?", taskId);
            if (userIds != null) {
                for (Integer userId : userIds) {
                    if (userId != null) {
                        insert(c, "INSERT INTO task_assignments (task_id, user_id, assigned_by) VALUES (?, ?, ?)",
                                taskId, userId, assignedBy);
                    }
                }
            }
            return null;
        });
    }

    /** Removes every assignment of a user inside a project (used when a member is removed). */
    public int removeAssignmentsForMember(int projectId, int userId) {
        return update("DELETE ta FROM task_assignments ta "
                    + "JOIN tasks t ON t.id = ta.task_id "
                    + "WHERE t.project_id = ? AND ta.user_id = ?", projectId, userId);
    }

    /* ============================== enrichment ============================== */

    /** Loads assignees and submission counters for a batch of tasks (2 queries total). */
    public void enrichTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(tasks.size(), "?"));
        Object[] ids = tasks.stream().map(Task::getId).toArray();

        Map<Integer, List<ProjectMember>> assignees = new HashMap<>();
        list("SELECT ta.task_id, ta.user_id, u.full_name, u.email FROM task_assignments ta "
           + "JOIN users u ON u.id = ta.user_id WHERE ta.task_id IN (" + placeholders + ") "
           + "ORDER BY u.full_name", (ResultSet rs) -> {
            int taskId = rs.getInt("task_id");
            ProjectMember m = assigneeMapper.map(rs);
            assignees.computeIfAbsent(taskId, k -> new ArrayList<>()).add(m);
            return null;
        }, ids);

        Map<Integer, int[]> counters = new HashMap<>();
        list("SELECT task_id, COUNT(*) AS total, "
           + "       SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending "
           + "FROM proof_submissions WHERE task_id IN (" + placeholders + ") GROUP BY task_id",
                (ResultSet rs) -> {
                    counters.put(rs.getInt("task_id"),
                            new int[]{rs.getInt("total"), rs.getInt("pending")});
                    return null;
                }, ids);

        for (Task t : tasks) {
            t.setAssignees(assignees.getOrDefault(t.getId(), new ArrayList<>()));
            int[] c = counters.getOrDefault(t.getId(), new int[]{0, 0});
            t.setSubmissionCount(c[0]);
            t.setPendingReviewCount(c[1]);
        }
    }

    /* ============================== utilities ============================== */

    /**
     * Whitelist for the ORDER BY clause. Values coming from the query string are
     * mapped to fixed SQL fragments, so ordering can never be used for injection.
     */
    private String orderByClause(String sortKey, String sortDir) {
        String column;
        String key = sortKey == null ? "" : sortKey.toLowerCase();
        switch (key) {
            case "title":    column = "t.title"; break;
            case "priority": column = "FIELD(t.priority, 'CRITICAL','HIGH','MEDIUM','LOW')"; break;
            case "status":   column = "FIELD(t.status, 'REJECTED','IN_REVIEW','SUBMITTED','IN_PROGRESS','TODO','APPROVED','COMPLETED')"; break;
            case "created":  column = "t.created_at"; break;
            case "due":
            default:         column = "t.due_date"; break;
        }
        String direction = "desc".equalsIgnoreCase(sortDir) ? " DESC" : " ASC";
        return " ORDER BY " + column + direction + ", t.id DESC";
    }
}

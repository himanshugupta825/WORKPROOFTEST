package com.workproof.dao;

import com.workproof.model.ActivityLog;
import com.workproof.model.DashboardStats;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Task;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregate queries behind the four role dashboards.
 *
 * Every number comes from a COUNT/SUM over real rows - the spec explicitly forbids
 * hardcoded metrics, and this DAO is the only place that produces them.
 *
 * Scope is expressed with a single SQL condition per role:
 *   ADMIN     -> no restriction
 *   MANAGER   -> projects where the user is a MANAGER member
 *   MEMBER    -> tasks assigned to the user
 *   REVIEWER  -> submissions of projects where the user is a REVIEWER member
 */
public class DashboardDao extends BaseDao {

    private final TaskDao taskDao = new TaskDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    private static final String MANAGER_SCOPE =
            " AND t.project_id IN (SELECT project_id FROM project_members WHERE user_id = ? AND role_in_project = 'MANAGER') ";
    private static final String MEMBER_SCOPE =
            " AND EXISTS (SELECT 1 FROM task_assignments ta WHERE ta.task_id = t.id AND ta.user_id = ?) ";
    private static final String REVIEWER_SCOPE =
            " AND t.project_id IN (SELECT project_id FROM project_members WHERE user_id = ? AND role_in_project = 'REVIEWER') ";

    /* ============================== ADMIN ============================== */

    public DashboardStats statsForAdmin() {
        DashboardStats stats = new DashboardStats();
        applyTaskCounts(stats, "", null);
        applySubmissionCounts(stats, "", null);

        stats.setTotalProjects(count("SELECT COUNT(*) FROM projects"));
        stats.setActiveProjects(count("SELECT COUNT(*) FROM projects WHERE status = 'ACTIVE'"));
        stats.setTotalUsers(count("SELECT COUNT(*) FROM users"));
        stats.setActiveUsers(count("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'"));
        stats.setTotalMembers(count("SELECT COUNT(*) FROM project_members"));

        stats.setUpcomingDeadlines(taskDao.findUpcomingDeadlinesAll(5));
        stats.setRecentActivity(activityLogDao.findRecent(8));
        stats.setAwaitingReviewList(pendingSubmissionsForScope("", null, 5));
        applyStatusBreakdown(stats, taskDao.countGroupedByStatusAll());
        return stats;
    }

    /* ============================= MANAGER ============================= */

    /** Manager of project X sees the tasks and submissions of all projects they manage. */
    public DashboardStats statsForManager(int userId) {
        DashboardStats stats = new DashboardStats();
        applyTaskCounts(stats, MANAGER_SCOPE, userId);
        applySubmissionCounts(stats, MANAGER_SCOPE, userId);

        stats.setTotalProjects(count("SELECT COUNT(*) FROM project_members WHERE user_id = ? AND role_in_project = 'MANAGER'",
                userId));
        stats.setActiveProjects(count("SELECT COUNT(*) FROM projects p JOIN project_members pm ON pm.project_id = p.id "
                + "WHERE pm.user_id = ? AND pm.role_in_project = 'MANAGER' AND p.status = 'ACTIVE'", userId));
        stats.setTotalMembers(count("SELECT COUNT(DISTINCT pm2.user_id) FROM project_members pm1 "
                + "JOIN project_members pm2 ON pm2.project_id = pm1.project_id "
                + "WHERE pm1.user_id = ? AND pm1.role_in_project = 'MANAGER'", userId));

        stats.setUpcomingDeadlines(upcomingDeadlinesForScope(MANAGER_SCOPE, userId, 5));
        stats.setRecentActivity(activityLogDao.findRecent(8));
        stats.setAwaitingReviewList(pendingSubmissionsForScope(MANAGER_SCOPE, userId, 5));
        stats.setPendingSubmissions(count(
                "SELECT COUNT(*) FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
              + "WHERE s.status = 'PENDING' " + MANAGER_SCOPE, userId));

        TaskDao dao = new TaskDao();
        stats.setTodoTasks(dao.countForScope(MANAGER_SCOPE, userId, "TODO"));
        stats.setInProgressTasks(dao.countForScope(MANAGER_SCOPE, userId, "IN_PROGRESS"));
        stats.setSubmittedTasks(dao.countForScope(MANAGER_SCOPE, userId, "SUBMITTED"));
        stats.setInReviewTasks(dao.countForScope(MANAGER_SCOPE, userId, "IN_REVIEW"));
        stats.setApprovedTasks(dao.countForScope(MANAGER_SCOPE, userId, "APPROVED"));
        stats.setRejectedTaskCount(dao.countForScope(MANAGER_SCOPE, userId, "REJECTED"));
        return stats;
    }

    /* ============================== MEMBER ============================== */

    /** A team member sees only the tasks assigned to them. */
    public DashboardStats statsForMember(int userId) {
        DashboardStats stats = new DashboardStats();
        applyTaskCounts(stats, MEMBER_SCOPE, userId);

        // For a member the "submission" counters describe their own uploads.
        stats.setTotalProjects(count("SELECT COUNT(*) FROM project_members WHERE user_id = ?", userId));
        stats.setActiveProjects(count("SELECT COUNT(*) FROM projects p JOIN project_members pm ON pm.project_id = p.id "
                + "WHERE pm.user_id = ? AND p.status = 'ACTIVE'", userId));
        stats.setApprovedSubmissions(count(
                "SELECT COUNT(*) FROM proof_submissions WHERE submitted_by = ? AND status = 'APPROVED'", userId));
        stats.setRejectedSubmissions(count(
                "SELECT COUNT(*) FROM proof_submissions WHERE submitted_by = ? AND status = 'REJECTED'", userId));
        stats.setPendingSubmissions(count(
                "SELECT COUNT(*) FROM proof_submissions WHERE submitted_by = ? AND status = 'PENDING'", userId));

        stats.setUpcomingDeadlines(taskDao.findUpcomingDeadlinesForUser(userId, 5));
        stats.setRecentActivity(activityLogDao.findRecentForUser(userId, 8));

        Map<String, Integer> byStatus = taskDao.countGroupedByStatusForUser(userId, true);
        applyStatusBreakdown(stats, byStatus);
        return stats;
    }

    /* ============================= REVIEWER ============================= */

    /** A reviewer sees the verification workload of the projects assigned to them. */
    public DashboardStats statsForReviewer(int userId) {
        DashboardStats stats = new DashboardStats();
        applyTaskCounts(stats, REVIEWER_SCOPE, userId);
        applySubmissionCounts(stats, REVIEWER_SCOPE, userId);

        stats.setTotalProjects(count("SELECT COUNT(*) FROM project_members WHERE user_id = ? AND role_in_project = 'REVIEWER'",
                userId));
        stats.setActiveProjects(count("SELECT COUNT(*) FROM projects p JOIN project_members pm ON pm.project_id = p.id "
                + "WHERE pm.user_id = ? AND pm.role_in_project = 'REVIEWER' AND p.status = 'ACTIVE'", userId));
        stats.setTotalMembers(count("SELECT COUNT(DISTINCT pm2.user_id) FROM project_members pm1 "
                + "JOIN project_members pm2 ON pm2.project_id = pm1.project_id "
                + "WHERE pm1.user_id = ? AND pm1.role_in_project = 'REVIEWER'", userId));

        stats.setUpcomingDeadlines(upcomingDeadlinesForScope(REVIEWER_SCOPE, userId, 5));
        stats.setRecentActivity(activityLogDao.findRecent(8));
        stats.setAwaitingReviewList(pendingSubmissionsForScope(REVIEWER_SCOPE, userId, 5));

        Map<String, Integer> byStatus = taskDao.countGroupedByStatusForUser(userId, false);
        applyStatusBreakdown(stats, byStatus);
        return stats;
    }

    /* ============================ shared parts ============================ */

    /** Fills the task counters using one pass over the tasks table. */
    private void applyTaskCounts(DashboardStats stats, String scopeCondition, Integer userId) {
        String sql = "SELECT COUNT(*) AS total, "
                   + " SUM(CASE WHEN t.status IN ('TODO','IN_PROGRESS') THEN 1 ELSE 0 END) AS pending, "
                   + " SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed, "
                   + " SUM(CASE WHEN t.status IN ('SUBMITTED','IN_REVIEW') THEN 1 ELSE 0 END) AS awaiting, "
                   + " SUM(CASE WHEN t.status = 'APPROVED' THEN 1 ELSE 0 END) AS approved, "
                   + " SUM(CASE WHEN t.status <> 'COMPLETED' AND t.due_date < CURDATE() THEN 1 ELSE 0 END) AS overdue "
                   + "FROM tasks t WHERE 1 = 1 " + scopeCondition;

        Object[] params = userId == null ? new Object[]{} : new Object[]{userId};
        one(sql, (ResultSet rs) -> {
            stats.setTotalTasks(rs.getInt("total"));
            stats.setPendingTasks(rs.getInt("pending"));
            stats.setCompletedTasks(rs.getInt("completed"));
            stats.setAwaitingVerification(rs.getInt("awaiting"));
            stats.setApprovedTasks(rs.getInt("approved"));
            stats.setOverdueTasks(rs.getInt("overdue"));
            return null;
        }, params);
    }

    /** Fills the evidence counters (approved / rejected / pending submissions). */
    private void applySubmissionCounts(DashboardStats stats, String scopeCondition, Integer userId) {
        String sql = "SELECT "
                   + " SUM(CASE WHEN s.status = 'APPROVED' THEN 1 ELSE 0 END) AS approved, "
                   + " SUM(CASE WHEN s.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected, "
                   + " SUM(CASE WHEN s.status = 'PENDING' THEN 1 ELSE 0 END) AS pending "
                   + "FROM proof_submissions s JOIN tasks t ON t.id = s.task_id WHERE 1 = 1 " + scopeCondition;

        Object[] params = userId == null ? new Object[]{} : new Object[]{userId};
        one(sql, (ResultSet rs) -> {
            stats.setApprovedSubmissions(rs.getInt("approved"));
            stats.setRejectedSubmissions(rs.getInt("rejected"));
            stats.setPendingSubmissions(rs.getInt("pending"));
            return null;
        }, params);
    }

    private void applyStatusBreakdown(DashboardStats stats, Map<String, Integer> byStatus) {
        stats.setTodoTasks(byStatus.getOrDefault("TODO", 0));
        stats.setInProgressTasks(byStatus.getOrDefault("IN_PROGRESS", 0));
        stats.setSubmittedTasks(byStatus.getOrDefault("SUBMITTED", 0));
        stats.setInReviewTasks(byStatus.getOrDefault("IN_REVIEW", 0));
        stats.setApprovedTasks(byStatus.getOrDefault("APPROVED", 0));
        stats.setRejectedTaskCount(byStatus.getOrDefault("REJECTED", 0));
    }

    private List<Task> upcomingDeadlinesForScope(String scopeCondition, Integer userId, int limit) {
        return list("SELECT t.id, t.project_id, t.title, t.description, t.priority, t.status, t.start_date, "
                  + "       t.due_date, t.created_by, t.completed_at, t.created_at, t.updated_at, "
                  + "       p.name AS project_name, p.code AS project_code, u.full_name AS creator_name "
                  + "FROM tasks t JOIN projects p ON p.id = t.project_id JOIN users u ON u.id = t.created_by "
                  + "WHERE t.status NOT IN ('COMPLETED') " + scopeCondition
                  + "ORDER BY t.due_date ASC LIMIT ?",
                (ResultSet rs) -> {
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
                },
                userId == null ? new Object[]{limit} : new Object[]{userId, limit});
    }

    /** Submissions awaiting a decision, limited to the caller's scope. */
    private List<ProofSubmission> pendingSubmissionsForScope(String scopeCondition, Integer userId, int limit) {
        String sql = "SELECT s.id, s.task_id, s.submitted_by, s.title, s.description, s.status, s.version, "
                   + "       s.submitted_at, s.updated_at, u.full_name AS submitter_name, t.title AS task_title, "
                   + "       t.status AS task_status, t.project_id, p.name AS project_name "
                   + "FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
                   + "JOIN projects p ON p.id = t.project_id JOIN users u ON u.id = s.submitted_by "
                   + "WHERE s.status = 'PENDING' " + scopeCondition + "ORDER BY s.submitted_at ASC LIMIT ?";

        List<ProofSubmission> result = list(sql, (ResultSet rs) -> {
            ProofSubmission s = new ProofSubmission();
            s.setId(rs.getInt("id"));
            s.setTaskId(rs.getInt("task_id"));
            s.setSubmittedBy(rs.getInt("submitted_by"));
            s.setTitle(rs.getString("title"));
            s.setDescription(rs.getString("description"));
            s.setStatus(rs.getString("status"));
            s.setVersion(rs.getInt("version"));
            s.setSubmittedAt(toLocalDateTime(rs.getTimestamp("submitted_at")));
            s.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            s.setSubmitterName(rs.getString("submitter_name"));
            s.setTaskTitle(rs.getString("task_title"));
            s.setTaskStatus(rs.getString("task_status"));
            s.setProjectId(rs.getInt("project_id"));
            s.setProjectName(rs.getString("project_name"));
            return s;
        }, userId == null ? new Object[]{limit} : new Object[]{userId, limit});
        return result == null ? new ArrayList<>() : result;
    }

    /** Activity feed of one project, shown on the project details page. */
    public List<ActivityLog> projectActivity(int projectId, int limit) {
        return activityLogDao.findRecentForProject(projectId, limit);
    }
}

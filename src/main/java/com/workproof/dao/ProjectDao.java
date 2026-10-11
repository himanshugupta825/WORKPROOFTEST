package com.workproof.dao;

import com.workproof.model.PagedResult;
import com.workproof.model.Project;
import com.workproof.model.ProjectMember;
import com.workproof.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for `projects` and `project_members`.
 *
 * Project statistics (task counts, member counts, progress) are read with two
 * extra aggregate queries for the whole page and then merged in Java, instead of
 * running one query per project. That keeps the listing pages fast and the SQL
 * readable.
 */
public class ProjectDao extends BaseDao {

    private static final String BASE_SELECT =
            "SELECT p.id, p.name, p.code, p.description, p.start_date, p.end_date, p.status, "
          + "       p.created_by, p.archived_at, p.created_at, p.updated_at, u.full_name AS owner_name "
          + "FROM projects p JOIN users u ON u.id = p.created_by ";

    private final RowMapper<Project> mapper = (ResultSet rs) -> {
        Project p = new Project();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setCode(rs.getString("code"));
        p.setDescription(rs.getString("description"));
        p.setStartDate(toLocalDate(rs.getDate("start_date")));
        p.setEndDate(toLocalDate(rs.getDate("end_date")));
        p.setStatus(rs.getString("status"));
        p.setCreatedBy(rs.getInt("created_by"));
        p.setArchivedAt(toLocalDateTime(rs.getTimestamp("archived_at")));
        p.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        p.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        p.setOwnerName(rs.getString("owner_name"));
        return p;
    };

    private final RowMapper<ProjectMember> memberMapper = (ResultSet rs) -> {
        ProjectMember m = new ProjectMember();
        m.setId(rs.getInt("id"));
        m.setProjectId(rs.getInt("project_id"));
        m.setUserId(rs.getInt("user_id"));
        m.setRoleInProject(rs.getString("role_in_project"));
        m.setJoinedAt(toLocalDateTime(rs.getTimestamp("joined_at")));
        m.setFullName(rs.getString("full_name"));
        m.setEmail(rs.getString("email"));
        m.setGlobalRoleName(rs.getString("global_role_name"));
        m.setDepartment(rs.getString("department"));
        m.setStatus(rs.getString("status"));
        return m;
    };

    private static final String MEMBER_SELECT =
            "SELECT pm.id, pm.project_id, pm.user_id, pm.role_in_project, pm.joined_at, "
          + "       u.full_name, u.email, u.department, u.status, r.name AS global_role_name "
          + "FROM project_members pm "
          + "JOIN users u ON u.id = pm.user_id "
          + "JOIN roles r ON r.id = u.role_id ";

    /* =============================== projects =============================== */

    public Project findById(int id) {
        Project project = one(BASE_SELECT + "WHERE p.id = ?", mapper, id);
        if (project != null) {
            enrich(List.of(project));
        }
        return project;
    }

    public Project findByCode(String code) {
        return one(BASE_SELECT + "WHERE p.code = ?", mapper, code);
    }

    public boolean existsByCode(String code, Integer exceptProjectId) {
        if (exceptProjectId == null) {
            return count("SELECT COUNT(*) FROM projects WHERE code = ?", code) > 0;
        }
        return count("SELECT COUNT(*) FROM projects WHERE code = ? AND id <> ?", code, exceptProjectId) > 0;
    }

    /**
     * Filtered, paged project listing.
     *
     * @param viewerId        when not null, only projects this user belongs to are returned
     * @param projectRoleFor  when not null together with viewerId, only projects where the
     *                        user has that project role (e.g. REVIEWER) are returned
     * @param search          free text matched against name and code
     * @param status          exact project status, or null/"ALL"
     * @param includeArchived when false, archived projects are hidden
     */
    public PagedResult<Project> search(Integer viewerId, String projectRoleFor, String search, String status,
                                       boolean includeArchived, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");

        if (viewerId != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = p.id AND pm.user_id = ? ");
            params.add(viewerId);
            if (projectRoleFor != null && !projectRoleFor.trim().isEmpty()) {
                where.append(" AND pm.role_in_project = ? ");
                params.add(projectRoleFor.trim());
            }
            where.append(") ");
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (p.name LIKE ? OR p.code LIKE ? OR p.description LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND p.status = ? ");
            params.add(status.trim());
        }
        if (!includeArchived) {
            where.append(" AND p.status <> 'ARCHIVED' ");
        }

        int total = count("SELECT COUNT(*) FROM projects p " + where, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<Project> projects = list(sql + where.toString() + " ORDER BY p.created_at DESC LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        enrich(projects);
        return new PagedResult<>(projects, total, page, pageSize);
    }

    /** Projects used on a dashboard: most recently updated first, limited. */
    public List<Project> findRecentForUser(int userId, int limit) {
        List<Project> projects = list(BASE_SELECT
                + "JOIN project_members pm ON pm.project_id = p.id AND pm.user_id = ? "
                + "WHERE p.status <> 'ARCHIVED' ORDER BY p.updated_at DESC LIMIT ?", mapper, userId, limit);
        enrich(projects);
        return projects;
    }

    /** All projects the user takes part in (no pagination) - used by reports and pickers. */
    public List<Project> findAllForUser(int userId) {
        List<Project> projects = list(BASE_SELECT
                + "JOIN project_members pm ON pm.project_id = p.id AND pm.user_id = ? "
                + "ORDER BY p.name", mapper, userId);
        enrich(projects);
        return projects;
    }

    public List<Project> findAll(boolean includeArchived) {
        List<Project> projects = list(BASE_SELECT
                + (includeArchived ? "" : "WHERE p.status <> 'ARCHIVED' ")
                + "ORDER BY p.created_at DESC", mapper);
        enrich(projects);
        return projects;
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM projects");
    }

    public int countByStatus(String status) {
        return count("SELECT COUNT(*) FROM projects WHERE status = ?", status);
    }

    public int countForUser(int userId) {
        return count("SELECT COUNT(*) FROM project_members WHERE user_id = ?", userId);
    }

    /**
     * Creates the project and its owner membership in ONE transaction, so a project
     * can never exist without at least one manager.
     */
    public int createWithOwner(final Project project, final int ownerUserId) {
        return tx((Connection c) -> {
            int projectId = insert(c, "INSERT INTO projects (name, code, description, start_date, end_date, "
                                   + "status, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    project.getName(), project.getCode(), project.getDescription(), project.getStartDate(),
                    project.getEndDate(), project.getStatus(), project.getCreatedBy());
            insert(c, "INSERT INTO project_members (project_id, user_id, role_in_project) VALUES (?, ?, 'MANAGER')",
                    projectId, ownerUserId);
            return projectId;
        });
    }

    public int update(Project project) {
        return update("UPDATE projects SET name = ?, code = ?, description = ?, start_date = ?, end_date = ?, "
                    + "status = ? WHERE id = ?",
                project.getName(), project.getCode(), project.getDescription(), project.getStartDate(),
                project.getEndDate(), project.getStatus(), project.getId());
    }

    public int updateStatus(int projectId, String status) {
        return update("UPDATE projects SET status = ? WHERE id = ?", status, projectId);
    }

    /** Soft delete: archived projects disappear from listings but history is preserved. */
    public int archive(int projectId) {
        return update("UPDATE projects SET status = 'ARCHIVED', archived_at = ? WHERE id = ?",
                LocalDateTime.now(), projectId);
    }

    public int restore(int projectId) {
        return update("UPDATE projects SET status = 'ACTIVE', archived_at = NULL WHERE id = ?", projectId);
    }

    /* =========================== project members =========================== */

    public List<ProjectMember> findMembers(int projectId) {
        return list(MEMBER_SELECT + "WHERE pm.project_id = ? ORDER BY "
                + "FIELD(pm.role_in_project, 'MANAGER', 'MEMBER', 'REVIEWER'), u.full_name",
                memberMapper, projectId);
    }

    public ProjectMember findMember(int projectId, int userId) {
        return one(MEMBER_SELECT + "WHERE pm.project_id = ? AND pm.user_id = ?", memberMapper, projectId, userId);
    }

    /** @return MANAGER / MEMBER / REVIEWER, or null when the user is not a member. */
    public String findProjectRole(int projectId, int userId) {
        ProjectMember member = findMember(projectId, userId);
        return member == null ? null : member.getRoleInProject();
    }

    public boolean isMember(int projectId, int userId) {
        return count("SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?",
                projectId, userId) > 0;
    }

    public int addMember(int projectId, int userId, String roleInProject) {
        // INSERT IGNORE-like behaviour via ON DUPLICATE KEY UPDATE keeps the UI idempotent.
        return update("INSERT INTO project_members (project_id, user_id, role_in_project) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE role_in_project = VALUES(role_in_project)",
                projectId, userId, roleInProject);
    }

    public int updateMemberRole(int projectId, int userId, String roleInProject) {
        return update("UPDATE project_members SET role_in_project = ? WHERE project_id = ? AND user_id = ?",
                roleInProject, projectId, userId);
    }

    /**
     * Removes a member. The owner (projects.created_by) cannot be removed here -
     * the service layer checks that rule before calling this method.
     */
    public int removeMember(int projectId, int userId) {
        return update("DELETE FROM project_members WHERE project_id = ? AND user_id = ?", projectId, userId);
    }

    public int countMembers(int projectId) {
        return count("SELECT COUNT(*) FROM project_members WHERE project_id = ?", projectId);
    }

    /** Managers of a project - they receive notifications about new submissions. */
    public List<ProjectMember> findManagers(int projectId) {
        return list(MEMBER_SELECT + "WHERE pm.project_id = ? AND pm.role_in_project = 'MANAGER'",
                memberMapper, projectId);
    }

    /** Reviewers attached to a project - they are notified about evidence to verify. */
    public List<ProjectMember> findReviewers(int projectId) {
        return list(MEMBER_SELECT + "WHERE pm.project_id = ? AND pm.role_in_project = 'REVIEWER'",
                memberMapper, projectId);
    }

    /**
     * Returns projectId -> roleInProject for one user and a batch of projects.
     * Used to decorate a listing page without running one query per row.
     */
    public Map<Integer, String> findProjectRolesForUser(int userId, List<Integer> projectIds) {
        Map<Integer, String> roles = new HashMap<>();
        if (projectIds == null || projectIds.isEmpty()) {
            return roles;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(projectIds.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(projectIds);
        list("SELECT project_id, role_in_project FROM project_members "
           + "WHERE user_id = ? AND project_id IN (" + placeholders + ")", (ResultSet rs) -> {
            roles.put(rs.getInt("project_id"), rs.getString("role_in_project"));
            return null;
        }, params.toArray());
        return roles;
    }

    /** Active users who are NOT yet members of the project (candidate list for the "add member" form). */
    public List<User> findCandidateUsers(int projectId) {
        return list("SELECT u.id, u.full_name, u.email, u.role_id, u.status, u.department, u.designation, "
                  + "       u.phone, u.last_login_at, u.created_at, r.name AS role_name "
                  + "FROM users u JOIN roles r ON r.id = u.role_id "
                  + "WHERE u.status = 'ACTIVE' AND r.name <> 'ADMIN' "
                  + "  AND NOT EXISTS (SELECT 1 FROM project_members pm "
                  + "                  WHERE pm.project_id = ? AND pm.user_id = u.id) "
                  + "ORDER BY u.full_name",
                (ResultSet rs) -> {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setEmail(rs.getString("email"));
                    u.setRoleId(rs.getInt("role_id"));
                    u.setRoleName(rs.getString("role_name"));
                    u.setStatus(rs.getString("status"));
                    u.setDepartment(rs.getString("department"));
                    u.setDesignation(rs.getString("designation"));
                    u.setPhone(rs.getString("phone"));
                    u.setLastLoginAt(toLocalDateTime(rs.getTimestamp("last_login_at")));
                    u.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    return u;
                }, projectId);
    }

    /* ============================= enrichment ============================= */

    /**
     * Fills taskCount / completedTaskCount / awaitingVerificationCount / overdueTaskCount
     * and memberCount for a batch of projects using two aggregate queries.
     */
    private void enrich(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(projects.size(), "?"));
        Object[] ids = projects.stream().map(Project::getId).toArray();

        Map<Integer, int[]> taskStats = new HashMap<>();
        list("SELECT project_id, COUNT(*) AS total_tasks, "
           + "       SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_tasks, "
           + "       SUM(CASE WHEN status IN ('SUBMITTED', 'IN_REVIEW') THEN 1 ELSE 0 END) AS awaiting_tasks, "
           + "       SUM(CASE WHEN status <> 'COMPLETED' AND due_date < CURDATE() THEN 1 ELSE 0 END) AS overdue_tasks "
           + "FROM tasks WHERE project_id IN (" + placeholders + ") GROUP BY project_id",
                (ResultSet rs) -> {
                    taskStats.put(rs.getInt("project_id"), new int[]{
                            rs.getInt("total_tasks"),
                            rs.getInt("completed_tasks"),
                            rs.getInt("awaiting_tasks"),
                            rs.getInt("overdue_tasks")});
                    return null;
                }, ids);

        Map<Integer, Integer> memberCounts = new HashMap<>();
        list("SELECT project_id, COUNT(*) AS members FROM project_members WHERE project_id IN ("
                        + placeholders + ") GROUP BY project_id",
                (ResultSet rs) -> {
                    memberCounts.put(rs.getInt("project_id"), rs.getInt("members"));
                    return null;
                }, ids);

        for (Project p : projects) {
            int[] stats = taskStats.getOrDefault(p.getId(), new int[]{0, 0, 0, 0});
            p.setTaskCount(stats[0]);
            p.setCompletedTaskCount(stats[1]);
            p.setAwaitingVerificationCount(stats[2]);
            p.setOverdueTaskCount(stats[3]);
            p.setMemberCount(memberCounts.getOrDefault(p.getId(), 0));
        }
    }

    /** Exposed for the report service: raw progress rows straight from the SQL view. */
    public List<Map<String, Object>> readProgressView() {
        return list("SELECT * FROM v_project_progress ORDER BY progress_percent DESC", (ResultSet rs) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("projectId", rs.getInt("project_id"));
            row.put("projectName", rs.getString("project_name"));
            row.put("projectCode", rs.getString("project_code"));
            row.put("projectStatus", rs.getString("project_status"));
            row.put("startDate", toLocalDate(rs.getDate("start_date")));
            row.put("endDate", toLocalDate(rs.getDate("end_date")));
            row.put("totalTasks", rs.getInt("total_tasks"));
            row.put("completedTasks", rs.getInt("completed_tasks"));
            row.put("approvedTasks", rs.getInt("approved_tasks"));
            row.put("awaitingTasks", rs.getInt("awaiting_verification_tasks"));
            row.put("rejectedTasks", rs.getInt("rejected_tasks"));
            row.put("overdueTasks", rs.getInt("overdue_tasks"));
            row.put("progressPercent", rs.getDouble("progress_percent"));
            return row;
        });
    }
}

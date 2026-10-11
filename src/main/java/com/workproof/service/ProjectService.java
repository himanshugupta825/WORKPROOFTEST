package com.workproof.service;

import com.workproof.dao.ProjectDao;
import com.workproof.dao.TaskDao;
import com.workproof.dao.UserDao;
import com.workproof.exception.NotFoundException;
import com.workproof.exception.ValidationException;
import com.workproof.model.PagedResult;
import com.workproof.model.Project;
import com.workproof.model.ProjectMember;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.model.enums.ProjectRole;
import com.workproof.model.enums.ProjectStatus;
import com.workproof.util.ValidationUtil;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Business rules for projects and project membership.
 *
 * Everything a servlet needs is exposed here, so the servlets stay thin:
 * they read request parameters, call one service method, and forward to a JSP.
 */
public class ProjectService {

    private final ProjectDao projectDao = new ProjectDao();
    private final TaskDao taskDao = new TaskDao();
    private final UserDao userDao = new UserDao();
    private final ActivityService activityService = new ActivityService();
    private final NotificationService notificationService = new NotificationService();

    /** Project roles that can be assigned from the UI. */
    public List<ProjectRole> assignableRoles() {
        return Arrays.asList(ProjectRole.MANAGER, ProjectRole.MEMBER, ProjectRole.REVIEWER);
    }

    public List<ProjectStatus> allStatuses() {
        return Arrays.asList(ProjectStatus.values());
    }

    /* =============================== queries =============================== */

    /**
     * Projects visible to the viewer.
     * Admins see everything; everybody else sees only projects they belong to.
     */
    public PagedResult<Project> listForViewer(User viewer, String search, String status,
                                             boolean includeArchived, int page, int pageSize) {
        Integer viewerId = viewer.isAdmin() ? null : viewer.getId();
        PagedResult<Project> result = projectDao.search(viewerId, null, search, status, includeArchived, page, pageSize);
        fillViewerRoles(viewer, result.getItems());
        return result;
    }

    /** All projects of a given user, used by task and report filters. */
    public List<Project> findProjectsForUser(User user) {
        if (user.isAdmin()) {
            List<Project> all = projectDao.findAll(true);
            fillViewerRoles(user, all);
            return all;
        }
        List<Project> mine = projectDao.findAllForUser(user.getId());
        fillViewerRoles(user, mine);
        return mine;
    }

    /**
     * Loads one project and checks that the viewer may open it.
     *
     * @throws NotFoundException      when the project does not exist
     * @throws com.workproof.exception.AuthorizationException when the viewer is not a member
     */
    public Project getForViewer(User viewer, int projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project #" + projectId + " was not found.");
        }
        String roleInProject = viewer.isAdmin() ? "ADMIN" : projectDao.findProjectRole(projectId, viewer.getId());
        Permissions.requireViewProject(viewer, project, roleInProject);
        project.setCurrentUserProjectRole(roleInProject);
        return project;
    }

    /** Project without an access check - only for internal service use (e.g. notifications). */
    public Project findById(int projectId) {
        return projectDao.findById(projectId);
    }

    /**
     * Project role of a user, or null when they are not a member.
     * Null-safe convenience used by servlets that only need a permission check.
     */
    public String findProjectRoleSafe(int projectId, int userId) {
        if (projectId <= 0) {
            return null;
        }
        try {
            return projectDao.findProjectRole(projectId, userId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public List<ProjectMember> findMembers(int projectId) {
        return projectDao.findMembers(projectId);
    }

    public List<User> findCandidateUsers(int projectId) {
        return projectDao.findCandidateUsers(projectId);
    }

    public List<Task> findTasks(int projectId) {
        return taskDao.findByProject(projectId);
    }

    public List<Task> findUpcomingDeadlines(int projectId, int limit) {
        return taskDao.findUpcomingDeadlinesForProject(projectId, limit);
    }

    /** Progress figures used by the project details page and the reports. */
    public Project withProgress(Project project) {
        Project fresh = projectDao.findById(project.getId());
        if (fresh != null) {
            fresh.setCurrentUserProjectRole(project.getCurrentUserProjectRole());
            return fresh;
        }
        return project;
    }

    /* ================================ writes ================================ */

    /**
     * Creates a project. The creator automatically becomes its manager/owner.
     * Only users who may manage projects (admin or manager) can create one.
     */
    public int create(User actor, String name, String code, String description, String startDate, String endDate,
                      String status, String ip) {

        Permissions.requireAdminOrManager(actor);

        name = ValidationUtil.require(name, "Project name", 3, 150);
        code = ValidationUtil.requireProjectCode(code);
        description = ValidationUtil.require(description, "Description", 10, 5000);
        LocalDate start = ValidationUtil.requireDate(startDate, "Start date");
        LocalDate end = ValidationUtil.requireDate(endDate, "End date");
        ValidationUtil.validateDateRange(start, end, "Start date", "End date");
        ProjectStatus projectStatus = ProjectStatus.from(pickStatus(status));

        if (projectDao.existsByCode(code, null)) {
            throw new ValidationException("Project code '" + code + "' is already in use. Please choose another one.");
        }

        Project project = new Project();
        project.setName(name);
        project.setCode(code);
        project.setDescription(description);
        project.setStartDate(start);
        project.setEndDate(end);
        project.setStatus(projectStatus.name());
        project.setCreatedBy(actor.getId());

        int projectId = projectDao.createWithOwner(project, actor.getId());

        activityService.log(actor, ActivityService.PROJECT_CREATED, "PROJECT", projectId,
                "Created project '" + name + "' (" + code + ")", ip);
        return projectId;
    }

    /** Updates project details. Only the owner, a project manager or an admin may do this. */
    public void update(User actor, int projectId, String name, String code, String description,
                       String startDate, String endDate, String status, String ip) {

        Project existing = projectDao.findById(projectId);
        if (existing == null) {
            throw new NotFoundException("Project #" + projectId + " was not found.");
        }
        String roleInProject = projectDao.findProjectRole(projectId, actor.getId());
        Permissions.requireManageProject(actor, existing, roleInProject);

        name = ValidationUtil.require(name, "Project name", 3, 150);
        code = ValidationUtil.requireProjectCode(code);
        description = ValidationUtil.require(description, "Description", 10, 5000);
        LocalDate start = ValidationUtil.requireDate(startDate, "Start date");
        LocalDate end = ValidationUtil.requireDate(endDate, "End date");
        ValidationUtil.validateDateRange(start, end, "Start date", "End date");
        ProjectStatus projectStatus = ProjectStatus.from(pickStatus(status));

        if (projectDao.existsByCode(code, projectId)) {
            throw new ValidationException("Project code '" + code + "' is already used by another project.");
        }

        Project updated = new Project();
        updated.setId(projectId);
        updated.setName(name);
        updated.setCode(code);
        updated.setDescription(description);
        updated.setStartDate(start);
        updated.setEndDate(end);
        updated.setStatus(projectStatus.name());
        projectDao.update(updated);

        activityService.log(actor, ActivityService.PROJECT_UPDATED, "PROJECT", projectId,
                "Updated project '" + name + "' (" + code + ")", ip);
    }

    /** Soft delete: the project and all its history stay in the database. */
    public void archive(User actor, int projectId, String ip) {
        Project project = requireManageable(actor, projectId);
        projectDao.archive(projectId);
        activityService.log(actor, ActivityService.PROJECT_ARCHIVED, "PROJECT", projectId,
                "Archived project '" + project.getName() + "'", ip);
    }

    public void restore(User actor, int projectId, String ip) {
        Project project = requireManageable(actor, projectId);
        projectDao.restore(projectId);
        activityService.log(actor, ActivityService.PROJECT_RESTORED, "PROJECT", projectId,
                "Restored project '" + project.getName() + "'", ip);
    }

    /* ============================== membership ============================== */

    public void addMember(User actor, int projectId, int userId, String roleInProject, String ip) {
        Project project = requireManageable(actor, projectId);

        User member = userDao.findById(userId);
        if (member == null) {
            throw new NotFoundException("User #" + userId + " was not found.");
        }
        if (!member.isActive()) {
            throw new ValidationException("Cannot add " + member.getFullName()
                    + ": the account is deactivated.");
        }
        if (projectDao.isMember(projectId, userId)) {
            throw new ValidationException(member.getFullName() + " is already a member of this project.");
        }

        ProjectRole role = ProjectRole.from(roleInProject);
        projectDao.addMember(projectId, userId, role.name());

        activityService.log(actor, ActivityService.PROJECT_MEMBER_ADDED, "PROJECT", projectId,
                "Added " + member.getFullName() + " to project '" + project.getName() + "' as " + role.getLabel(), ip);
        notificationService.notifyProjectMembership(userId, projectId, project.getName(),
                role.getLabel(), actor);
    }

    /**
     * Removes a member and (optionally) releases their task assignments.
     * The project owner can never be removed - a project must always keep a manager.
     */
    public void removeMember(User actor, int projectId, int userId, String ip) {
        Project project = requireManageable(actor, projectId);

        if (project.getCreatedBy() == userId) {
            throw new ValidationException("The project owner cannot be removed from the project. "
                    + "Transfer ownership by editing the project first.");
        }
        ProjectMember member = projectDao.findMember(projectId, userId);
        if (member == null) {
            throw new ValidationException("That user is not a member of this project.");
        }

        projectDao.removeMember(projectId, userId);
        int releasedTasks = taskDao.removeAssignmentsForMember(projectId, userId);

        activityService.log(actor, ActivityService.PROJECT_MEMBER_REMOVED, "PROJECT", projectId,
                "Removed " + member.getFullName() + " from project '" + project.getName()
                        + "' (released " + releasedTasks + " task assignment(s))", ip);
    }

    public void changeMemberRole(User actor, int projectId, int userId, String roleInProject, String ip) {
        Project project = requireManageable(actor, projectId);

        ProjectMember member = projectDao.findMember(projectId, userId);
        if (member == null) {
            throw new ValidationException("That user is not a member of this project.");
        }
        if (project.getCreatedBy() == userId && !ProjectRole.MANAGER.name().equals(roleInProject)) {
            throw new ValidationException("The project owner must keep the manager role.");
        }

        ProjectRole role = ProjectRole.from(roleInProject);
        projectDao.updateMemberRole(projectId, userId, role.name());

        activityService.log(actor, ActivityService.PROJECT_MEMBER_ROLE_CHANGED, "PROJECT", projectId,
                "Changed " + member.getFullName() + "'s project role to " + role.getLabel()
                        + " in '" + project.getName() + "'", ip);
    }

    /** Manager ids plus reviewer ids of a project - recipients of submission notifications. */
    public List<Integer> findReviewerIds(int projectId) {
        List<Integer> ids = new java.util.ArrayList<>();
        for (ProjectMember manager : projectDao.findManagers(projectId)) {
            ids.add(manager.getUserId());
        }
        for (ProjectMember reviewer : projectDao.findReviewers(projectId)) {
            if (!ids.contains(reviewer.getUserId())) {
                ids.add(reviewer.getUserId());
            }
        }
        return ids;
    }

    /* ================================ helpers ================================ */

    /** Loads a project and throws unless the actor may manage it. */
    private Project requireManageable(User actor, int projectId) {
        Project project = projectDao.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project #" + projectId + " was not found.");
        }
        String roleInProject = projectDao.findProjectRole(projectId, actor.getId());
        Permissions.requireManageProject(actor, project, roleInProject);
        return project;
    }

    /**
     * The status posted by the form is validated against the allowed values.
     * An empty value keeps the project in PLANNED (new projects) - this helper makes
     * that rule explicit instead of hiding it in a ternary.
     */
    private String pickStatus(String status) {
        if (ValidationUtil.trimToNull(status) == null) {
            return ProjectStatus.PLANNED.name();
        }
        return ValidationUtil.requireOneOf(status, "Status",
                "PLANNED", "ACTIVE", "ON_HOLD", "COMPLETED", "ARCHIVED");
    }

    /**
     * Fills "currentUserProjectRole" so the UI can hide actions the user cannot perform.
     * One single query resolves the roles for the whole page.
     */
    private void fillViewerRoles(User viewer, List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        if (viewer.isAdmin()) {
            for (Project project : projects) {
                project.setCurrentUserProjectRole("ADMIN");
            }
            return;
        }
        List<Integer> ids = projects.stream().map(Project::getId).collect(java.util.stream.Collectors.toList());
        java.util.Map<Integer, String> roles = projectDao.findProjectRolesForUser(viewer.getId(), ids);
        for (Project project : projects) {
            project.setCurrentUserProjectRole(roles.get(project.getId()));
        }
    }

    public boolean canManage(User viewer, Project project) {
        return Permissions.canManageProject(viewer, project, project.getCurrentUserProjectRole());
    }
}

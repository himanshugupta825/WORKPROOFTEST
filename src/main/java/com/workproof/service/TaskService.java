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
import com.workproof.model.enums.TaskPriority;
import com.workproof.model.enums.TaskStatus;
import com.workproof.util.ValidationUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Business rules for tasks: creation, assignment, editing and status transitions.
 *
 * The status machine lives in {@link TaskStatus#canMoveTo(TaskStatus)} so the rule
 * "you cannot jump from To Do straight to Approved" is enforced in exactly one place
 * and can be unit tested.
 */
public class TaskService {

    private final TaskDao taskDao = new TaskDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final UserDao userDao = new UserDao();
    private final ActivityService activityService = new ActivityService();
    private final NotificationService notificationService = new NotificationService();

    public List<TaskStatus> allStatuses() {
        return Arrays.asList(TaskStatus.values());
    }

    public List<TaskPriority> allPriorities() {
        return Arrays.asList(TaskPriority.values());
    }

    /* =============================== queries =============================== */

    /**
     * Task listing with filters.
     *
     * A team member only sees tasks of their projects; the "assigned to me" filter is
     * pre-selected for members, and they can still see what the rest of their team is
     * doing (read only).
     */
    public PagedResult<Task> list(User viewer, Integer projectId, String status, String priority,
                                  String search, String scope, String sortKey, String sortDir,
                                  int page, int pageSize) {

        // "mine" restricts the list to tasks assigned to the logged-in user.
        Integer assigneeFilter = "mine".equalsIgnoreCase(scope) ? viewer.getId() : null;

        // Filters are validated against the allowed values, so an unknown status cannot
        // silently return "everything" and confuse the user.
        String statusFilter = normaliseFilter(status, statuses());
        String priorityFilter = normaliseFilter(priority, priorities());

        // Administrators have system-wide visibility.
        if (viewer.isAdmin()) {
            return taskDao.search(assigneeFilter, projectId, statusFilter, priorityFilter, search,
                    sortKey, sortDir, page, pageSize);
        }

        // Everybody else is limited to the projects they belong to.
        List<Integer> projectIds = new ArrayList<>();
        for (Project project : projectDao.findAllForUser(viewer.getId())) {
            if (projectId == null || projectId == project.getId()) {
                projectIds.add(project.getId());
            }
        }
        if (projectIds.isEmpty()) {
            return PagedResult.empty(pageSize);
        }
        return taskDao.searchInProjects(assigneeFilter, projectIds, statusFilter, priorityFilter, search,
                sortKey, sortDir, page, pageSize);
    }

    /** Single task plus the viewer's rights inside its project. */
    public Task getForViewer(User viewer, int taskId) {
        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task #" + taskId + " was not found.");
        }
        Project project = projectDao.findById(task.getProjectId());
        String roleInProject = viewer.isAdmin() ? "ADMIN" : projectDao.findProjectRole(project.getId(), viewer.getId());
        Permissions.requireViewProject(viewer, project, roleInProject);
        return task;
    }

    public Task findById(int taskId) {
        return taskDao.findById(taskId);
    }

    public List<Task> findOpenForUser(int userId, int limit) {
        return taskDao.findOpenForUser(userId, limit);
    }

    public List<Task> findUpcomingDeadlines(int userId, int limit) {
        return taskDao.findUpcomingDeadlinesForUser(userId, limit);
    }

    public List<ProjectMember> findAssignees(int taskId) {
        return taskDao.findAssignees(taskId);
    }

    /* ================================ writes ================================ */

    public int create(User actor, int projectId, String title, String description, String priority,
                      String startDate, String dueDate, List<Integer> assigneeIds, String ip) {

        Project project = projectDao.findById(projectId);
        if (project == null) {
            throw new NotFoundException("Project #" + projectId + " was not found.");
        }
        String roleInProject = projectDao.findProjectRole(projectId, actor.getId());
        Permissions.requireManageProject(actor, project, roleInProject);

        title = ValidationUtil.require(title, "Task title", 4, 180);
        description = ValidationUtil.require(description, "Description", 10, 5000);
        TaskPriority taskPriority = TaskPriority.from(
                ValidationUtil.requireOneOf(priority, "Priority", "LOW", "MEDIUM", "HIGH", "CRITICAL"));
        LocalDate start = ValidationUtil.optionalDate(startDate, "Start date");
        LocalDate due = ValidationUtil.requireDate(dueDate, "Deadline");
        if (start != null && due.isBefore(start)) {
            throw new ValidationException("The deadline cannot be before the start date.");
        }

        List<Integer> assignees = validatedAssignees(projectId, assigneeIds);

        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(taskPriority.name());
        task.setStatus(TaskStatus.TODO.name());
        task.setStartDate(start);
        task.setDueDate(due);
        task.setCreatedBy(actor.getId());

        int taskId = taskDao.createWithAssignees(task, assignees, actor.getId());
        task.setId(taskId);
        task.setProjectName(project.getName());

        activityService.log(actor, ActivityService.TASK_CREATED, "TASK", taskId,
                "Created task '" + title + "' in project '" + project.getName() + "'", ip);
        if (!assignees.isEmpty()) {
            notificationService.notifyTaskAssigned(task, assignees, actor);
            activityService.log(actor, ActivityService.TASK_ASSIGNED, "TASK", taskId,
                    "Assigned '" + title + "' to " + assignees.size() + " member(s)", ip);
        }
        return taskId;
    }

    /** Updates task metadata. Members may never call this - only managers/admins. */
    public void update(User actor, int taskId, String title, String description, String priority,
                       String startDate, String dueDate, List<Integer> assigneeIds, String ip) {

        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task #" + taskId + " was not found.");
        }
        Project project = projectDao.findById(task.getProjectId());
        String roleInProject = projectDao.findProjectRole(project.getId(), actor.getId());
        Permissions.requireEditTask(actor, task, project, roleInProject);

        title = ValidationUtil.require(title, "Task title", 4, 180);
        description = ValidationUtil.require(description, "Description", 10, 5000);
        TaskPriority taskPriority = TaskPriority.from(
                ValidationUtil.requireOneOf(priority, "Priority", "LOW", "MEDIUM", "HIGH", "CRITICAL"));
        LocalDate start = ValidationUtil.optionalDate(startDate, "Start date");
        LocalDate due = ValidationUtil.requireDate(dueDate, "Deadline");
        if (start != null && due.isBefore(start)) {
            throw new ValidationException("The deadline cannot be before the start date.");
        }

        List<Integer> previousAssignees = taskDao.findAssigneeIds(taskId);
        List<Integer> assignees = validatedAssignees(task.getProjectId(), assigneeIds);

        Task updated = new Task();
        updated.setId(taskId);
        updated.setTitle(title);
        updated.setDescription(description);
        updated.setPriority(taskPriority.name());
        updated.setStartDate(start);
        updated.setDueDate(due);
        taskDao.update(updated);
        taskDao.replaceAssignees(taskId, assignees, actor.getId());

        activityService.log(actor, ActivityService.TASK_UPDATED, "TASK", taskId,
                "Updated task '" + title + "'", ip);

        // Notify people who were added by this edit (not the ones who were already there).
        List<Integer> newlyAssigned = new ArrayList<>(assignees);
        newlyAssigned.removeAll(previousAssignees);
        if (!newlyAssigned.isEmpty()) {
            Task refreshed = taskDao.findById(taskId);
            notificationService.notifyTaskAssigned(refreshed, newlyAssigned, actor);
            activityService.log(actor, ActivityService.TASK_ASSIGNED, "TASK", taskId,
                    "Assigned the task to " + newlyAssigned.size() + " new member(s)", ip);
        }
    }

    /**
     * Moves a task to a new status after checking both the permission and the
     * allowed transition.
     */
    public void changeStatus(User actor, int taskId, String newStatusValue, String comment, String ip) {
        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task #" + taskId + " was not found.");
        }
        Project project = projectDao.findById(task.getProjectId());
        String roleInProject = projectDao.findProjectRole(project.getId(), actor.getId());
        TaskStatus current = task.getTaskStatus();
        TaskStatus target = TaskStatus.from(newStatusValue);

        if (current == target) {
            return;   // nothing to do, not an error
        }
        if (!current.canMoveTo(target)) {
            throw new ValidationException("A task cannot move from " + current.getLabel()
                    + " to " + target.getLabel() + ". Allowed next states: " + describeAllowed(current) + ".");
        }

        List<Integer> assigneeIds = taskDao.findAssigneeIds(taskId);
        boolean isAssignee = assigneeIds.contains(actor.getId());
        Permissions.requireTaskStatusChange(actor, task, project, roleInProject, isAssignee, target);

        if (target == TaskStatus.APPROVED || target == TaskStatus.REJECTED) {
            // Only the verification workflow may set these states - see ReviewService.
            throw new ValidationException("Approved and Rejected are set through the evidence review screen, "
                    + "not from the task page.");
        }

        taskDao.updateStatus(taskId, target.name());

        Task refreshed = taskDao.findById(taskId);
        activityService.log(actor, ActivityService.TASK_STATUS_CHANGED, "TASK", taskId,
                "Moved '" + task.getTitle() + "' from " + current.getLabel() + " to " + target.getLabel()
                        + (comment == null || comment.isBlank() ? "" : " - note: " + comment), ip);
        notificationService.notifyTaskStatusChanged(refreshed, actor, current.getLabel(), target.getLabel());
    }

    /** Managers may delete a task that has no submission history. */
    public void delete(User actor, int taskId, String ip) {
        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task #" + taskId + " was not found.");
        }
        Project project = projectDao.findById(task.getProjectId());
        String roleInProject = projectDao.findProjectRole(project.getId(), actor.getId());
        Permissions.requireManageProject(actor, project, roleInProject);

        if (task.getSubmissionCount() > 0) {
            throw new ValidationException("This task already has evidence submissions, so it cannot be "
                    + "deleted. Remove it from the board by setting the project to ARCHIVED instead.");
        }
        taskDao.delete(taskId);
        activityService.log(actor, ActivityService.TASK_DELETED, "TASK", taskId,
                "Deleted task '" + task.getTitle() + "' from project '" + project.getName() + "'", ip);
    }

    /* ================================ helpers ================================ */

    /**
     * Keeps only assignees who are members of the project, so a task can never be
     * assigned to an outsider.
     */
    private List<Integer> validatedAssignees(int projectId, List<Integer> assigneeIds) {
        List<Integer> valid = new ArrayList<>();
        if (assigneeIds == null) {
            return valid;
        }
        for (Integer userId : assigneeIds) {
            if (userId == null || valid.contains(userId)) {
                continue;
            }
            if (userDao.findById(userId) == null) {
                throw new ValidationException("User #" + userId + " does not exist.");
            }
            if (!projectDao.isMember(projectId, userId)) {
                throw new ValidationException("Only project members can be assigned a task. "
                        + "Add the person to the project first.");
            }
            valid.add(userId);
        }
        return valid;
    }

    private List<String> statuses() {
        List<String> values = new ArrayList<>();
        for (TaskStatus s : TaskStatus.values()) {
            values.add(s.name());
        }
        return values;
    }

    private List<String> priorities() {
        List<String> values = new ArrayList<>();
        for (TaskPriority p : TaskPriority.values()) {
            values.add(p.name());
        }
        return values;
    }

    /** Returns null (meaning "no filter") when the value is empty or not recognised. */
    private String normaliseFilter(String value, List<String> allowed) {
        String clean = ValidationUtil.trimToNull(value);
        if (clean == null || "ALL".equalsIgnoreCase(clean)) {
            return null;
        }
        for (String candidate : allowed) {
            if (candidate.equalsIgnoreCase(clean)) {
                return candidate;
            }
        }
        return null;
    }

    private String describeAllowed(TaskStatus status) {
        StringBuilder sb = new StringBuilder();
        for (TaskStatus next : status.allowedNext()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(next.getLabel());
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    /* ============================ dashboard widgets ============================ */

    public List<Task> upcomingDeadlinesForUser(int userId, int limit) {
        return taskDao.findUpcomingDeadlinesForUser(userId, limit);
    }

    public List<Task> tasksDueWithin(int days) {
        return taskDao.findDueWithinDays(days);
    }

    public List<Task> overdueTasks() {
        return taskDao.findOverdueTasks();
    }
}

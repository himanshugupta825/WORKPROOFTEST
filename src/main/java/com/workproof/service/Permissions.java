package com.workproof.service;

import com.workproof.exception.AuthorizationException;
import com.workproof.model.Project;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.model.enums.ProjectRole;
import com.workproof.model.enums.RoleName;
import com.workproof.model.enums.TaskStatus;

import java.util.List;

/**
 * The single place where WorkProof decides who may do what.
 *
 * Server-side enforcement (spec section 3) happens in the service layer, so even a
 * hand-crafted HTTP request that never touches the UI is stopped here. The JSP pages
 * only hide buttons; they are never the security boundary.
 *
 * Rules implemented (documented so they can be defended in a viva):
 *
 *   ADMIN
 *     - full access to every project, task and submission.
 *
 *   Project MANAGER (project_members.role_in_project = 'MANAGER') or project owner
 *     - edit project details, add/remove members
 *     - create/edit tasks, assign them, change any task status
 *     - review evidence in that project, but NOT their own submission.
 *
 *   Project MEMBER
 *     - view the project and its tasks
 *     - update the status of tasks assigned to them (To Do <-> In Progress)
 *     - submit evidence for tasks assigned to them
 *     - never edit task metadata (title, description, priority, dates)
 *
 *   Reviewer (project_members.role_in_project = 'REVIEWER')
 *     - view the project, inspect evidence, approve or reject
 *     - cannot change task metadata or project membership
 *     - cannot approve evidence they submitted themselves
 */
public final class Permissions {

    private Permissions() {
        // utility class
    }

    /* ============================ project access ============================ */

    /** Admin sees everything; everybody else must be a member of the project. */
    public static boolean canViewProject(User user, String projectRoleInProject) {
        if (user == null) {
            return false;
        }
        return user.isAdmin() || projectRoleInProject != null;
    }

    public static void requireViewProject(User user, Project project, String projectRoleInProject) {
        if (!canViewProject(user, projectRoleInProject)) {
            throw new AuthorizationException("You do not have access to project '" + project.getName() + "'.");
        }
    }

    /** Manage = edit details, manage members, create/edit tasks. */
    public static boolean canManageProject(User user, Project project, String projectRoleInProject) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        if (project != null && project.getCreatedBy() == user.getId()) {
            return true;
        }
        return ProjectRole.MANAGER.name().equals(projectRoleInProject);
    }

    public static void requireManageProject(User user, Project project, String projectRoleInProject) {
        if (!canManageProject(user, project, projectRoleInProject)) {
            throw new AuthorizationException("Only the project manager (or an administrator) may change "
                    + "the project '" + project.getName() + "'.");
        }
    }

    /** Managers and reviewers of a project may inspect submissions. */
    public static boolean canReviewInProject(User user, String projectRoleInProject) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        return ProjectRole.MANAGER.name().equals(projectRoleInProject)
                || ProjectRole.REVIEWER.name().equals(projectRoleInProject);
    }

    public static void requireReviewPermission(User user, String projectRoleInProject, String projectName) {
        if (!canReviewInProject(user, projectRoleInProject)) {
            throw new AuthorizationException("You are not a reviewer on project '" + projectName
                    + "', so you cannot decide on its evidence.");
        }
    }

    /* ============================== task access ============================== */

    public static boolean canEditTask(User user, Task task, Project project, String projectRoleInProject) {
        // Task metadata may only be changed by managers and admins.
        return canManageProject(user, project, projectRoleInProject);
    }

    public static void requireEditTask(User user, Task task, Project project, String projectRoleInProject) {
        if (!canEditTask(user, task, project, projectRoleInProject)) {
            throw new AuthorizationException("Only the project manager may edit task '" + task.getTitle() + "'.");
        }
    }

    /** A member may only move a task they are assigned to, and only between "work" states. */
    public static boolean canChangeTaskStatus(User user, Task task, Project project,
                                              String projectRoleInProject, boolean isAssignee,
                                              TaskStatus target) {
        if (user == null) {
            return false;
        }
        if (canManageProject(user, project, projectRoleInProject)) {
            return true;                      // manager / admin: any legal transition
        }
        if (!isAssignee) {
            return false;                     // members cannot move other people's tasks
        }
        if (ProjectRole.REVIEWER.name().equals(projectRoleInProject)) {
            return false;                     // reviewers decide through the review screen
        }
        // Members work on their own tasks and may never mark work as approved/completed.
        return target == TaskStatus.TODO
                || target == TaskStatus.IN_PROGRESS
                || target == TaskStatus.SUBMITTED
                || target == TaskStatus.IN_REVIEW;
    }

    public static void requireTaskStatusChange(User user, Task task, Project project,
                                               String projectRoleInProject, boolean isAssignee, TaskStatus target) {
        if (!canChangeTaskStatus(user, task, project, projectRoleInProject, isAssignee, target)) {
            throw new AuthorizationException("You are not allowed to move task '" + task.getTitle()
                    + "' to " + target.getLabel() + ".");
        }
    }

    public static boolean isAssignee(User user, List<Integer> assigneeIds) {
        return user != null && assigneeIds != null && assigneeIds.contains(user.getId());
    }

    /** Evidence may be submitted by an assignee, the project manager or an admin. */
    public static boolean canSubmitEvidence(User user, Project project, String projectRoleInProject,
                                           boolean isAssignee) {
        if (user == null) {
            return false;
        }
        if (canManageProject(user, project, projectRoleInProject)) {
            return true;
        }
        return isAssignee && !ProjectRole.REVIEWER.name().equals(projectRoleInProject);
    }

    public static void requireSubmitEvidence(User user, Task task, Project project,
                                             String projectRoleInProject, boolean isAssignee) {
        if (!canSubmitEvidence(user, project, projectRoleInProject, isAssignee)) {
            throw new AuthorizationException("You can only submit evidence for tasks assigned to you. "
                    + "Task: '" + task.getTitle() + "'.");
        }
    }

    /* =========================== account management =========================== */

    public static void requireAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new AuthorizationException("This action is restricted to administrators.");
        }
    }

    public static void requireAdminOrManager(User user) {
        if (user == null || (!user.isAdmin() && !user.isManager())) {
            throw new AuthorizationException("This action is restricted to project managers and administrators.");
        }
    }

    /** Registration is limited to the three "worker" roles; admins are seeded or created by an admin. */
    public static boolean isSelfRegisterableRole(RoleName roleName) {
        return roleName == RoleName.MANAGER || roleName == RoleName.MEMBER || roleName == RoleName.REVIEWER;
    }

    /** Global role check helpers used by servlets for coarse-grained screens. */
    public static void requireRole(User user, RoleName... allowed) {
        if (user == null) {
            throw new AuthorizationException("Please sign in to continue.");
        }
        for (RoleName role : allowed) {
            if (user.getRole() == role) {
                return;
            }
        }
        throw new AuthorizationException("Your role does not have access to this page.");
    }
}

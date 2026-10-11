package com.workproof.controller;

import com.workproof.model.Project;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.ActivityService;
import com.workproof.service.Permissions;
import com.workproof.service.ProjectService;
import com.workproof.service.SubmissionService;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Task details: description, assignees, status timeline, evidence history and the
 * actions this user is allowed to perform (the buttons are hidden for actions the
 * service layer would reject anyway).
 */
@WebServlet(name = "TaskViewServlet", urlPatterns = {"/tasks/view"})
public class TaskViewServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TaskService taskService = new TaskService();
    private final ProjectService projectService = new ProjectService();
    private final SubmissionService submissionService = new SubmissionService();
    private final ActivityService activityService = new ActivityService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int taskId = requiredIntParam(request, "id", "Task");

        try {
            Task task = taskService.getForViewer(user, taskId);
            Project project = projectService.findById(task.getProjectId());
            String roleInProject = user.isAdmin() ? "ADMIN"
                    : projectService.findProjectRoleSafe(task.getProjectId(), user.getId());

            List<Integer> assigneeIds = taskService.findAssignees(taskId).stream()
                    .map(m -> m.getUserId()).collect(java.util.stream.Collectors.toList());

            boolean isAssignee = assigneeIds.contains(user.getId());
            boolean canEdit = Permissions.canEditTask(user, task, project, roleInProject);
            boolean canSubmit = Permissions.canSubmitEvidence(user, project, roleInProject, isAssignee);
            boolean canMove = Permissions.canChangeTaskStatus(user, task, project, roleInProject, isAssignee,
                    com.workproof.model.enums.TaskStatus.IN_PROGRESS)
                    || Permissions.canManageProject(user, project, roleInProject);

            List<ProofSubmission> history = submissionService.historyForTask(user, task);

            request.setAttribute("task", task);
            request.setAttribute("project", project);
            request.setAttribute("assignees", taskService.findAssignees(taskId));
            request.setAttribute("isAssignee", isAssignee);
            request.setAttribute("canEdit", canEdit);
            request.setAttribute("canSubmit", canSubmit);
            request.setAttribute("canChangeStatus", canMove);
            request.setAttribute("submissions", history);
            request.setAttribute("activity", activityService.recentForEntity("TASK", taskId, 10));
            request.setAttribute("statuses", taskService.allStatuses());
            render(request, response, "tasks/view.jsp");

        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

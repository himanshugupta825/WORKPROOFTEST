package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.Project;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.Permissions;
import com.workproof.service.ProjectService;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Create/edit task form. Only project managers and administrators can reach it -
 * a team member who guesses the URL gets the 403 page from the service layer.
 */
@WebServlet(name = "TaskFormServlet", urlPatterns = {"/tasks/new", "/tasks/edit"})
public class TaskFormServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TaskService taskService = new TaskService();
    private final ProjectService projectService = new ProjectService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        boolean editing = "/tasks/edit".equals(request.getServletPath());

        try {
            Project project;
            Task task = null;

            if (editing) {
                int taskId = requiredIntParam(request, "id", "Task");
                task = taskService.getForViewer(user, taskId);
                project = projectService.findById(task.getProjectId());
                String roleInProject = user.isAdmin() ? "ADMIN"
                        : projectService.findProjectRoleSafe(project.getId(), user.getId());
                if (!Permissions.canEditTask(user, task, project, roleInProject)) {
                    sendForbidden(request, response, "Only the project manager can edit this task.");
                    return;
                }
            } else {
                int projectId = requiredIntParam(request, "projectId", "Project");
                project = projectService.getForViewer(user, projectId);
                if (!projectService.canManage(user, project)) {
                    sendForbidden(request, response,
                            "Only the project manager can create tasks in this project.");
                    return;
                }
            }

            request.setAttribute("task", task);
            request.setAttribute("project", project);
            request.setAttribute("members", projectService.findMembers(project.getId()));
            request.setAttribute("assigneeIds", task == null ? List.of() : task.getAssignees().stream()
                    .map(m -> m.getUserId()).collect(java.util.stream.Collectors.toList()));
            request.setAttribute("priorities", taskService.allPriorities());
            request.setAttribute("mode", editing ? "edit" : "create");
            render(request, response, "tasks/form.jsp");

        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        boolean editing = "/tasks/edit".equals(request.getServletPath());
        Integer taskId = intParam(request, "id");
        Integer projectId = intParam(request, "projectId");
        List<Integer> assignees = intParams(request, "assigneeIds");

        try {
            if (editing) {
                if (taskId == null) {
                    throw new ValidationException("Task id is missing.");
                }
                taskService.update(user, taskId, param(request, "title"), param(request, "description"),
                        param(request, "priority"), param(request, "startDate"), param(request, "dueDate"),
                        assignees, clientIp(request));
                redirectWithSuccess(request, response, "/tasks/view?id=" + taskId, "Task updated successfully.");
            } else {
                if (projectId == null) {
                    throw new ValidationException("Project id is missing.");
                }
                int newId = taskService.create(user, projectId, param(request, "title"),
                        param(request, "description"), param(request, "priority"), param(request, "startDate"),
                        param(request, "dueDate"), assignees, clientIp(request));
                redirectWithSuccess(request, response, "/tasks/view?id=" + newId,
                        "Task created and assigned to the selected members.");
            }
        } catch (ValidationException e) {
            showFormAgain(request, response, editing, taskId, projectId, assignees, e.getMessage());
        } catch (RuntimeException e) {
            if (handleServiceException(request, response, e, true)) {
                return;
            }
            showFormAgain(request, response, editing, taskId, projectId, assignees, e.getMessage());
        }
    }

    private void showFormAgain(HttpServletRequest request, HttpServletResponse response, boolean editing,
                               Integer taskId, Integer projectId, List<Integer> assignees, String message)
            throws ServletException, IOException {

        Task draft = new Task();
        draft.setId(taskId == null ? 0 : taskId);
        draft.setProjectId(projectId == null ? 0 : projectId);
        draft.setTitle(param(request, "title"));
        draft.setDescription(param(request, "description"));
        draft.setPriority(param(request, "priority"));
        draft.setStartDate(dateParam(request, "startDate"));
        draft.setDueDate(dateParam(request, "dueDate"));

        Project project = projectService.findById(projectId == null ? 0 : projectId);
        request.setAttribute("task", draft);
        request.setAttribute("project", project);
        request.setAttribute("members", project == null ? List.of() : projectService.findMembers(project.getId()));
        request.setAttribute("assigneeIds", assignees);
        request.setAttribute("priorities", taskService.allPriorities());
        request.setAttribute("formError", message);
        request.setAttribute("mode", editing ? "edit" : "create");
        render(request, response, "tasks/form.jsp");
    }
}

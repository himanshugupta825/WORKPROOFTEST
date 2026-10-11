package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.Project;
import com.workproof.model.User;
import com.workproof.service.Permissions;
import com.workproof.service.ProjectService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Create and edit project form (URLs /projects/new and /projects/edit?id=...).
 * The same servlet handles both because the fields are identical - this reduces
 * duplicated code and keeps the two forms in sync.
 */
@WebServlet(name = "ProjectFormServlet", urlPatterns = {"/projects/new", "/projects/edit"})
public class ProjectFormServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ProjectService projectService = new ProjectService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String path = request.getServletPath();

        if ("/projects/edit".equals(path)) {
            int projectId = requiredIntParam(request, "id", "Project");
            try {
                Project project = projectService.getForViewer(user, projectId);
                if (!projectService.canManage(user, project)) {
                    sendForbidden(request, response,
                            "Only the project manager or an administrator can edit this project.");
                    return;
                }
                request.setAttribute("project", project);
            } catch (RuntimeException e) {
                handleServiceException(request, response, e, true);
                return;
            }
        } else {
            // Creating a project is limited to project managers and administrators.
            try {
                Permissions.requireAdminOrManager(user);
            } catch (RuntimeException e) {
                handleServiceException(request, response, e, true);
                return;
            }
        }

        request.setAttribute("statuses", projectService.allStatuses());
        request.setAttribute("mode", "/projects/edit".equals(path) ? "edit" : "create");
        render(request, response, "projects/form.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        boolean editing = "/projects/edit".equals(request.getServletPath());
        Integer projectId = intParam(request, "id");

        try {
            if (editing) {
                if (projectId == null) {
                    throw new ValidationException("Project id is missing.");
                }
                projectService.update(user, projectId,
                        param(request, "name"), param(request, "code"), param(request, "description"),
                        param(request, "startDate"), param(request, "endDate"), param(request, "status"),
                        clientIp(request));
                redirectWithSuccess(request, response, "/projects/view?id=" + projectId,
                        "Project updated successfully.");
            } else {
                int newId = projectService.create(user,
                        param(request, "name"), param(request, "code"), param(request, "description"),
                        param(request, "startDate"), param(request, "endDate"), param(request, "status"),
                        clientIp(request));
                redirectWithSuccess(request, response, "/projects/view?id=" + newId,
                        "Project created. Add team members to get started.");
            }
        } catch (ValidationException e) {
            showFormAgain(request, response, editing, projectId, e.getMessage());
        } catch (RuntimeException e) {
            if (handleServiceException(request, response, e, true)) {
                return;
            }
            showFormAgain(request, response, editing, projectId, e.getMessage());
        }
    }

    /** Re-renders the form with the submitted values and the error message. */
    private void showFormAgain(HttpServletRequest request, HttpServletResponse response,
                               boolean editing, Integer projectId, String message)
            throws ServletException, IOException {

        Project draft = new Project();
        draft.setId(projectId == null ? 0 : projectId);
        draft.setName(param(request, "name"));
        draft.setCode(param(request, "code"));
        draft.setDescription(param(request, "description"));
        draft.setStartDate(dateParam(request, "startDate"));
        draft.setEndDate(dateParam(request, "endDate"));
        draft.setStatus(param(request, "status"));

        request.setAttribute("project", draft);
        request.setAttribute("formError", message);
        request.setAttribute("statuses", projectService.allStatuses());
        request.setAttribute("mode", editing ? "edit" : "create");
        render(request, response, "projects/form.jsp");
    }
}

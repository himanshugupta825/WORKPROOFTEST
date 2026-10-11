package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.User;
import com.workproof.service.ProjectService;
import com.workproof.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * All state-changing project operations that are not the create/edit form:
 * archive, restore, and membership management.
 *
 * A single endpoint with an "action" parameter keeps the URL space small; every
 * branch re-checks permissions inside the service layer.
 */
@WebServlet(name = "ProjectActionServlet", urlPatterns = {"/projects/action"})
public class ProjectActionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ProjectService projectService = new ProjectService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String action = param(request, "action");
        Integer projectId = intParam(request, "projectId");
        String backTo = projectId == null ? "/projects" : "/projects/view?id=" + projectId;

        if (projectId == null) {
            redirectWithError(request, response, "/projects", "Project id is missing.");
            return;
        }

        try {
            switch (action == null ? "" : action) {
                case "archive":
                    projectService.archive(user, projectId, clientIp(request));
                    redirectWithSuccess(request, response, "/projects",
                            "Project archived. It stays in the database but is hidden from the listings.");
                    return;

                case "restore":
                    projectService.restore(user, projectId, clientIp(request));
                    redirectWithSuccess(request, response, backTo + "&restored=1",
                            "Project restored and active again.");
                    return;

                case "addMember": {
                    Integer userId = intParam(request, "userId");
                    if (userId == null) {
                        throw new ValidationException("Please choose a user to add.");
                    }
                    projectService.addMember(user, projectId, userId, param(request, "roleInProject"),
                            clientIp(request));
                    redirectWithSuccess(request, response, backTo, "Member added to the project.");
                    return;
                }

                case "removeMember": {
                    Integer userId = intParam(request, "userId");
                    if (userId == null) {
                        throw new ValidationException("User id is missing.");
                    }
                    projectService.removeMember(user, projectId, userId, clientIp(request));
                    redirectWithSuccess(request, response, backTo,
                            "Member removed. Their task assignments in this project were released.");
                    return;
                }

                case "changeRole": {
                    Integer userId = intParam(request, "userId");
                    if (userId == null) {
                        throw new ValidationException("User id is missing.");
                    }
                    projectService.changeMemberRole(user, projectId, userId, param(request, "roleInProject"),
                            clientIp(request));
                    redirectWithSuccess(request, response, backTo, "Project role updated.");
                    return;
                }

                default:
                    redirectWithError(request, response, backTo, "Unknown project action: "
                            + ValidationUtil.require(action, "Action"));
            }
        } catch (ValidationException e) {
            redirectWithError(request, response, backTo, e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

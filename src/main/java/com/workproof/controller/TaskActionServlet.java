package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.User;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Task actions that change state: status transition and delete.
 * Both go through the service layer, which enforces the status machine and permissions.
 */
@WebServlet(name = "TaskActionServlet", urlPatterns = {"/tasks/action"})
public class TaskActionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final TaskService taskService = new TaskService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        Integer taskId = intParam(request, "taskId");
        if (taskId == null) {
            redirectWithError(request, response, "/tasks", "Task id is missing.");
            return;
        }
        String action = param(request, "action");
        String backTo = "/tasks/view?id=" + taskId;

        try {
            switch (action == null ? "" : action) {
                case "status":
                    String newStatus = param(request, "status");
                    taskService.changeStatus(user, taskId, newStatus, param(request, "comment"),
                            clientIp(request));
                    redirectWithSuccess(request, response, backTo,
                            "Task moved to " + com.workproof.model.enums.TaskStatus.from(newStatus).getLabel() + ".");
                    return;

                case "delete":
                    taskService.delete(user, taskId, clientIp(request));
                    redirectWithSuccess(request, response, "/tasks",
                            "Task deleted. The project history still contains the audit entry.");
                    return;

                default:
                    redirectWithError(request, response, backTo, "Unknown task action.");
            }
        } catch (ValidationException e) {
            redirectWithError(request, response, backTo, e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

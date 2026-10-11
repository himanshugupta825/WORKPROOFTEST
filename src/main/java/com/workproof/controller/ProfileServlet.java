package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.ActivityService;
import com.workproof.service.AuthService;
import com.workproof.service.SubmissionService;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Profile page: personal details, password change, my tasks and my evidence summary.
 * The "action" hidden field distinguishes the two forms on the page.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();
    private final TaskService taskService = new TaskService();
    private final SubmissionService submissionService = new SubmissionService();
    private final ActivityService activityService = new ActivityService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        List<Task> myTasks = taskService.findOpenForUser(user.getId(), 10);

        request.setAttribute("user", user);
        request.setAttribute("myTasks", myTasks);
        request.setAttribute("assignedCount", myTasks.size());
        request.setAttribute("approvedCount",
                submissionService.countForSubmitter(user.getId(), "APPROVED"));
        request.setAttribute("pendingCount",
                submissionService.countForSubmitter(user.getId(), "PENDING"));
        request.setAttribute("rejectedCount",
                submissionService.countForSubmitter(user.getId(), "REJECTED"));
        request.setAttribute("myActivity", activityService.recentForUser(user.getId(), 10));
        render(request, response, "profile/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String action = param(request, "action");

        try {
            if ("password".equals(action)) {
                authService.changePassword(user,
                        request.getParameter("currentPassword"),
                        request.getParameter("newPassword"),
                        request.getParameter("confirmPassword"),
                        clientIp(request));
                redirectWithSuccess(request, response, "/profile",
                        "Password changed successfully. Use it the next time you sign in.");
            } else {
                authService.updateProfile(user, param(request, "fullName"), param(request, "department"),
                        param(request, "designation"), param(request, "phone"), clientIp(request));
                redirectWithSuccess(request, response, "/profile", "Profile updated.");
            }
        } catch (ValidationException e) {
            redirectWithError(request, response, "/profile", e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

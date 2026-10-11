package com.workproof.controller;

import com.workproof.exception.AuthorizationException;
import com.workproof.model.ActivityLog;
import com.workproof.model.PagedResult;
import com.workproof.model.User;
import com.workproof.service.ActivityService;
import com.workproof.service.Permissions;
import com.workproof.service.UserService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

/**
 * System activity log for administrators: filter by user, action and date,
 * with every entry showing who did what and when.
 */
@WebServlet(name = "AdminActivityServlet", urlPatterns = {"/admin/activity"})
public class AdminActivityServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 25;

    private final ActivityService activityService = new ActivityService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        try {
            Permissions.requireAdmin(user);

            String search = param(request, "search");
            String action = param(request, "action");
            Integer userId = intParam(request, "userId");
            LocalDate from = dateParam(request, "from");
            LocalDate to = dateParam(request, "to");
            int page = WebUtil.pageParam(request);

            PagedResult<ActivityLog> logs = activityService.search(search, action, userId, from, to,
                    page, PAGE_SIZE);

            request.setAttribute("activityPage", logs);
            request.setAttribute("users", userService.findAll());
            request.setAttribute("actionTypes", new String[]{
                    "ALL",
                    ActivityService.AUTH_LOGIN, ActivityService.AUTH_LOGIN_FAILED, ActivityService.AUTH_LOGOUT,
                    ActivityService.AUTH_REGISTER, ActivityService.PASSWORD_CHANGED, ActivityService.PROFILE_UPDATED,
                    ActivityService.PROJECT_CREATED, ActivityService.PROJECT_UPDATED, ActivityService.PROJECT_ARCHIVED,
                    ActivityService.PROJECT_MEMBER_ADDED, ActivityService.PROJECT_MEMBER_REMOVED,
                    ActivityService.TASK_CREATED, ActivityService.TASK_UPDATED, ActivityService.TASK_STATUS_CHANGED,
                    ActivityService.TASK_ASSIGNED, ActivityService.TASK_DELETED,
                    ActivityService.SUBMISSION_CREATED, ActivityService.SUBMISSION_FILE_DOWNLOADED,
                    ActivityService.REVIEW_APPROVED, ActivityService.REVIEW_REJECTED,
                    ActivityService.USER_CREATED, ActivityService.USER_UPDATED, ActivityService.USER_STATUS_CHANGED,
                    ActivityService.USER_ROLE_CHANGED, ActivityService.USER_PASSWORD_RESET});
            request.setAttribute("search", search);
            request.setAttribute("action", ValidationUtil.trimToNull(action) == null ? "ALL" : action);
            request.setAttribute("selectedUserId", userId);
            request.setAttribute("from", from);
            request.setAttribute("to", to);
            render(request, response, "admin/activity.jsp");

        } catch (AuthorizationException e) {
            sendForbidden(request, response, e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

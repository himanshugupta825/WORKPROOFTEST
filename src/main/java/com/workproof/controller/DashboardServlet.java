package com.workproof.controller;

import com.workproof.model.DashboardStats;
import com.workproof.model.User;
import com.workproof.service.DashboardService;
import com.workproof.service.NotificationService;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The single dashboard entry point. The service picks the correct SQL scope for the
 * user's role, and the same JSP renders the numbers with small role adjustments.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final DashboardService dashboardService = new DashboardService();
    private final TaskService taskService = new TaskService();
    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        DashboardStats stats = dashboardService.statsFor(user);

        request.setAttribute("stats", stats);
        request.setAttribute("upcomingTasks",
                user.isAdmin() || user.isManager() || user.isReviewer()
                        ? stats.getUpcomingDeadlines()
                        : taskService.findUpcomingDeadlines(user.getId(), 5));
        request.setAttribute("recentNotifications", notificationService.recentForUser(user.getId(), 5));
        request.setAttribute("recentActivity", stats.getRecentActivity());
        render(request, response, "dashboard/index.jsp");
    }
}

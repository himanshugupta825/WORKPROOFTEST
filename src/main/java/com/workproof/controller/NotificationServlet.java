package com.workproof.controller;

import com.workproof.model.Notification;
import com.workproof.model.PagedResult;
import com.workproof.model.User;
import com.workproof.service.NotificationService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Notification centre: list, filter, mark one/all as read, delete.
 * Every operation is scoped by user id in SQL, so one user can never touch another
 * user's notifications, even by guessing an id.
 */
@WebServlet(name = "NotificationServlet", urlPatterns = {"/notifications", "/notifications/action"})
public class NotificationServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 12;

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String type = param(request, "type");
        boolean unreadOnly = WebUtil.boolParam(request, "unread");
        int page = WebUtil.pageParam(request);

        PagedResult<Notification> notifications = notificationService.search(user.getId(), unreadOnly, type,
                page, PAGE_SIZE);

        request.setAttribute("notificationPage", notifications);
        request.setAttribute("unreadOnly", unreadOnly);
        request.setAttribute("type", ValidationUtil.trimToNull(type) == null ? "ALL" : type);
        request.setAttribute("unreadCount", notificationService.unreadCount(user.getId()));
        request.setAttribute("types", com.workproof.model.enums.NotificationType.values());
        render(request, response, "notifications/list.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String action = param(request, "action");

        switch (action == null ? "" : action) {
            case "read": {
                Integer id = intParam(request, "id");
                if (id != null) {
                    notificationService.markAsRead(id, user.getId());
                }
                // If a link is provided, jump straight to the related page.
                String link = ValidationUtil.trimToNull(param(request, "link"));
                if (link != null && link.startsWith("/")) {
                    redirect(request, response, link);
                } else {
                    redirect(request, response, "/notifications");
                }
                return;
            }
            case "readAll":
                notificationService.markAllAsRead(user.getId());
                redirectWithSuccess(request, response, "/notifications", "All notifications marked as read.");
                return;

            case "delete": {
                Integer id = intParam(request, "id");
                if (id != null) {
                    notificationService.delete(id, user.getId());
                }
                redirectWithSuccess(request, response, "/notifications", "Notification deleted.");
                return;
            }
            default:
                redirectWithError(request, response, "/notifications", "Unknown notification action.");
        }
    }
}

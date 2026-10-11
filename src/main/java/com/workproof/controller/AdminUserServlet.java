package com.workproof.controller;

import com.workproof.exception.AuthorizationException;
import com.workproof.exception.ValidationException;
import com.workproof.model.PagedResult;
import com.workproof.model.User;
import com.workproof.model.enums.RoleName;
import com.workproof.model.enums.UserStatus;
import com.workproof.service.Permissions;
import com.workproof.service.UserService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Administrator user management: list, create, edit, activate/deactivate, reset password.
 *
 * The whole servlet starts with Permissions.requireAdmin(), so the role check happens
 * once, in one obvious place, for every request it handles.
 */
@WebServlet(name = "AdminUserServlet", urlPatterns = {"/admin/users", "/admin/users/action"})
public class AdminUserServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 10;

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        try {
            Permissions.requireAdmin(user);

            if ("/admin/users/action".equals(request.getServletPath())) {
                redirect(request, response, "/admin/users");
                return;
            }

            String search = param(request, "search");
            String role = param(request, "role");
            String status = param(request, "status");
            int page = WebUtil.pageParam(request);
            Integer editId = intParam(request, "edit");

            PagedResult<User> users = userService.search(search, role, status, page, PAGE_SIZE);

            request.setAttribute("userPage", users);
            request.setAttribute("roles", userService.allRoles());
            request.setAttribute("statuses", UserStatus.values());
            request.setAttribute("search", search);
            request.setAttribute("role", ValidationUtil.trimToNull(role) == null ? "ALL" : role);
            request.setAttribute("status", ValidationUtil.trimToNull(status) == null ? "ALL" : status);
            request.setAttribute("editUser", editId == null ? null : userService.findById(editId));
            request.setAttribute("totalUsers", userService.countAll());
            request.setAttribute("activeUsers", userService.countActive());
            request.setAttribute("adminCount", userService.countByRole(RoleName.ADMIN));
            request.setAttribute("managerCount", userService.countByRole(RoleName.MANAGER));
            request.setAttribute("memberCount", userService.countByRole(RoleName.MEMBER));
            request.setAttribute("reviewerCount", userService.countByRole(RoleName.REVIEWER));
            render(request, response, "admin/users.jsp");

        } catch (AuthorizationException e) {
            sendForbidden(request, response, e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User actor = currentUser(request);
        String action = param(request, "action");

        try {
            Permissions.requireAdmin(actor);

            switch (action == null ? "" : action) {
                case "create":
                    userService.create(actor, param(request, "fullName"), param(request, "email"),
                            request.getParameter("password"), request.getParameter("confirmPassword"),
                            param(request, "role"), param(request, "department"),
                            param(request, "designation"), param(request, "phone"), clientIp(request));
                    redirectWithSuccess(request, response, "/admin/users",
                            "User account created and notified.");
                    return;

                case "update": {
                    Integer userId = intParam(request, "userId");
                    if (userId == null) {
                        throw new ValidationException("User id is missing.");
                    }
                    userService.update(actor, userId, param(request, "fullName"), param(request, "role"),
                            param(request, "status"), param(request, "department"),
                            param(request, "designation"), param(request, "phone"), clientIp(request));
                    redirectWithSuccess(request, response, "/admin/users", "User account updated.");
                    return;
                }

                case "toggleStatus": {
                    Integer userId = intParam(request, "userId");
                    if (userId == null) {
                        throw new ValidationException("User id is missing.");
                    }
                    User target = userService.findById(userId);
                    if (target == null) {
                        throw new ValidationException("That user no longer exists.");
                    }
                    String newStatus = UserStatus.ACTIVE.name().equals(target.getStatus())
                            ? UserStatus.INACTIVE.name() : UserStatus.ACTIVE.name();
                    userService.changeStatus(actor, userId, newStatus, clientIp(request));
                    redirectWithSuccess(request, response, "/admin/users",
                            target.getFullName() + " is now " + newStatus.toLowerCase() + ".");
                    return;
                }

                case "resetPassword": {
                    Integer userId = intParam(request, "userId");
                    if (userId == null) {
                        throw new ValidationException("User id is missing.");
                    }
                    userService.resetPassword(actor, userId, request.getParameter("newPassword"),
                            request.getParameter("confirmPassword"), clientIp(request));
                    redirectWithSuccess(request, response, "/admin/users",
                            "Password reset. The new password is not shown anywhere - share it securely.");
                    return;
                }

                default:
                    redirectWithError(request, response, "/admin/users", "Unknown user action.");
            }
        } catch (AuthorizationException e) {
            sendForbidden(request, response, e.getMessage());
        } catch (ValidationException e) {
            redirectWithError(request, response, "/admin/users", e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

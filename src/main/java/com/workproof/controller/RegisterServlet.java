package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.User;
import com.workproof.model.enums.RoleName;
import com.workproof.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Self-registration screen. Only the three working roles can be chosen -
 * administrator accounts are created by an existing administrator or by seed data.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    /** Roles a visitor may pick, with a short explanation shown on the form. */
    private static final List<RoleName> SELF_SERVICE_ROLES =
            Arrays.asList(RoleName.MEMBER, RoleName.MANAGER, RoleName.REVIEWER);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (currentUser(request) != null) {
            redirect(request, response, "/dashboard");
            return;
        }
        request.setAttribute("roles", SELF_SERVICE_ROLES);
        render(request, response, "auth/register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            User user = authService.register(
                    param(request, "fullName"),
                    param(request, "email"),
                    request.getParameter("password"),
                    request.getParameter("confirmPassword"),
                    param(request, "role"),
                    param(request, "department"),
                    param(request, "designation"),
                    param(request, "phone"),
                    clientIp(request));

            redirectWithSuccess(request, response, "/login",
                    "Account created for " + user.getEmail() + ". Please sign in to continue.");

        } catch (ValidationException e) {
            keepEnteredValues(request);
            request.setAttribute("formError", e.getMessage());
            request.setAttribute("roles", SELF_SERVICE_ROLES);
            render(request, response, "auth/register.jsp");
        } catch (RuntimeException e) {
            if (handleServiceException(request, response, e, true)) {
                return;
            }
            keepEnteredValues(request);
            request.setAttribute("formError", e.getMessage());
            request.setAttribute("roles", SELF_SERVICE_ROLES);
            render(request, response, "auth/register.jsp");
        }
    }

    /** Puts the typed values back into the request so the user does not retype everything. */
    private void keepEnteredValues(HttpServletRequest request) {
        for (String field : new String[]{"fullName", "email", "role", "department", "designation", "phone"}) {
            request.setAttribute(field, param(request, field));
        }
    }
}

package com.workproof.controller;

import com.workproof.model.User;
import com.workproof.service.AuthService;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Secure logout.
 *
 * The logout is performed with a POST form that carries the CSRF token, so a
 * malicious site cannot log the user out with an innocent-looking link. The
 * GET handler does not log out; it just sends the user back to the dashboard.
 *
 * The whole session is invalidated (not only the user attribute), so no cached
 * personal data survives.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        redirect(request, response, currentUser(request) == null ? "/login" : "/dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        authService.recordLogout(user, clientIp(request));

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        HttpSession fresh = request.getSession(true);
        fresh.setAttribute(WebUtil.FLASH_SUCCESS, "You have been signed out safely.");
        redirect(request, response, "/login");
    }
}

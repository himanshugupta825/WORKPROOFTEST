package com.workproof.controller;

import com.workproof.exception.AuthenticationException;
import com.workproof.model.User;
import com.workproof.service.AuthService;
import com.workproof.util.AppConfig;
import com.workproof.util.CsrfUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Sign-in screen.
 *
 * Security highlights:
 *   - the session is INVALIDATED and a new one created after a successful login
 *     (session fixation protection),
 *   - a fresh CSRF token is generated for the new session,
 *   - the failed attempt message never reveals whether the email exists,
 *   - the post-login destination is validated as a local path.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (currentUser(request) != null) {
            redirect(request, response, "/dashboard");
            return;
        }
        render(request, response, "auth/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = param(request, "email");
        String password = request.getParameter("password");   // never trimmed: spaces can be part of a password
        String ip = clientIp(request);

        try {
            User user = authService.login(email, password, ip);

            // ---- session fixation protection: throw the old session away ----
            HttpSession oldSession = request.getSession(false);
            String redirectTarget = null;
            if (oldSession != null) {
                Object target = oldSession.getAttribute("redirectAfterLogin");
                if (target instanceof String) {
                    redirectTarget = (String) target;
                }
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(WebUtil.SESSION_USER, user);
            session.setMaxInactiveInterval(AppConfig.sessionTimeoutMinutes() * 60);
            session.setAttribute(CsrfUtil.SESSION_ATTR, CsrfUtil.newToken());

            WebUtil.flashSuccess(request, "Welcome back, " + user.getFullName() + "!");
            redirect(request, response, redirectTarget != null ? redirectTarget : "/dashboard");

        } catch (AuthenticationException e) {
            request.setAttribute("formError", e.getMessage());
            request.setAttribute("email", email);
            render(request, response, "auth/login.jsp");
        } catch (RuntimeException e) {
            if (handleServiceException(request, response, e, true)) {
                return;
            }
            request.setAttribute("formError", e.getMessage());
            request.setAttribute("email", email);
            render(request, response, "auth/login.jsp");
        }
    }
}

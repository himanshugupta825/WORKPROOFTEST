package com.workproof.filter;

import com.workproof.model.User;
import com.workproof.service.NotificationService;
import com.workproof.util.CsrfUtil;
import com.workproof.util.WebUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Session-based authentication gate.
 *
 * - Public URLs (landing page, login, register, static assets, error pages) pass through.
 * - Every other URL requires a valid session containing a User object; otherwise the
 *   visitor is redirected to the login page and the originally requested URL is kept
 *   so they land there after signing in.
 * - On every authenticated request the CSRF token and the unread notification counter
 *   are refreshed in the session, which is what the header bell shows.
 */
public class AuthenticationFilter implements Filter {

    /** URL prefixes that never require a session. */
    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
            "/assets/",
            "/login",
            "/register",
            "/access-denied",
            "/error",
            "/favicon.ico",
            "/robots.txt");

    /** Exact paths that are public. */
    private static final List<String> PUBLIC_EXACT = Arrays.asList("/", "/index.jsp");

    private final NotificationService notificationService = new NotificationService();

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to configure
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }

        User user = WebUtil.currentUser(httpRequest);

        if (user == null && !isPublic(path)) {
            // Remember where the user wanted to go, then send them to the login page.
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("redirectAfterLogin", safeRedirectTarget(httpRequest));
            WebUtil.flashError(httpRequest, "Please sign in to continue.");
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        if (user != null) {
            HttpSession session = httpRequest.getSession(true);
            // Token for the state-changing forms of this page.
            CsrfUtil.getToken(session);
            // Unread badge for the notification bell.
            try {
                session.setAttribute("unreadNotifications", notificationService.unreadCount(user.getId()));
            } catch (RuntimeException e) {
                // A database hiccup must not block navigation.
                session.setAttribute("unreadNotifications", 0);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Only relative URLs are accepted, so a crafted "redirectAfterLogin" can never
     * bounce the user to an external site (open-redirect protection).
     */
    private String safeRedirectTarget(HttpServletRequest request) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if (uri.isEmpty() || uri.contains("..") || uri.startsWith("//") || uri.contains("://")) {
            return null;
        }
        String query = request.getQueryString();
        String target = uri + (query == null ? "" : "?" + query);
        return target.length() > 400 ? uri : target;
    }

    private boolean isPublic(String path) {
        if (PUBLIC_EXACT.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // nothing to release
    }
}

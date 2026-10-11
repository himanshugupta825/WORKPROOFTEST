package com.workproof.filter;

import com.workproof.util.CsrfUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Blocks cross-site request forgery.
 *
 * Every POST/PUT/DELETE/PATCH request must carry the per-session token that the JSPs
 * embed as a hidden field:
 *
 *   &lt;input type="hidden" name="_csrf" value="${sessionScope.csrfToken}"&gt;
 *
 * The token is compared with a constant-time comparison. A mismatch returns HTTP 403
 * with a friendly page instead of silently doing the action.
 *
 * This filter must run AFTER AuthenticationFilter (so a session exists) and BEFORE
 * the servlets.
 */
public class CsrfFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to configure
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (requiresToken(httpRequest)) {
            if (!CsrfUtil.isValid(httpRequest)) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpRequest.setAttribute("errorTitle", "Security check failed");
                httpRequest.setAttribute("errorMessage",
                        "Your form session expired or the request did not come from WorkProof. "
                      + "Please go back, reload the page and try again.");
                httpRequest.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(httpRequest, httpResponse);
                return;
            }
        } else if (isPageRequest(httpRequest)) {
            // Safe request for an HTML page: make sure a token exists BEFORE the JSP renders,
            // otherwise forms such as the login form would render an empty hidden field and
            // every first POST would be rejected.
            CsrfUtil.getToken(httpRequest);
        }

        chain.doFilter(request, response);
    }

    /**
     * True for browser page requests (GET/HEAD) that are not static assets, i.e. requests
     * whose HTML may contain a form and therefore needs a token in the session.
     */
    private boolean isPageRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return !path.startsWith("/assets/");
    }

    /** State-changing HTTP methods need the token; safe methods do not. */
    private boolean requiresToken(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    @Override
    public void destroy() {
        // nothing to release
    }
}

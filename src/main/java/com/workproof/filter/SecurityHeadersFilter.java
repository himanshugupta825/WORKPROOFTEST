package com.workproof.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Adds the standard browser-security headers to every response.
 *
 * - X-Content-Type-Options: nosniff      -> browsers must not guess content types
 * - X-Frame-Options + frame-ancestors    -> the app cannot be framed (clickjacking)
 * - Referrer-Policy                      -> URLs are not leaked to other sites
 * - Content-Security-Policy              -> scripts may only load from this origin,
 *                                           which defeats most XSS injections. Inline
 *                                           STYLES are allowed because progress bars
 *                                           set a width style; inline SCRIPTS are not.
 * - Cache-Control on private pages       -> the back button after logout does not
 *                                           show cached personal data.
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to configure
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse http = (HttpServletResponse) response;
        http.setHeader("X-Content-Type-Options", "nosniff");
        http.setHeader("X-Frame-Options", "DENY");
        http.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        http.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        http.setHeader("Content-Security-Policy",
                "default-src 'self'; "
              + "img-src 'self' data:; "
              + "style-src 'self' 'unsafe-inline'; "
              + "script-src 'self'; "
              + "font-src 'self' data:; "
              + "form-action 'self'; "
              + "frame-ancestors 'none'; "
              + "base-uri 'self'");

        // Never cache pages that contain personal data.
        if (http.getHeader("Cache-Control") == null) {
            http.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            http.setHeader("Pragma", "no-cache");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to release
    }
}

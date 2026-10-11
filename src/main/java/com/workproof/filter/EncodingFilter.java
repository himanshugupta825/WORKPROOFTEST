package com.workproof.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Forces UTF-8 for every request and response.
 *
 * Without this, Indian names and any non-ASCII text in forms would arrive mangled
 * because Servlet 4.0 still defaults request encoding to ISO-8859-1.
 * It must be the FIRST filter in web.xml.
 */
public class EncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to configure
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to release
    }
}

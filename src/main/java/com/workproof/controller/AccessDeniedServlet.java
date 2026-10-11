package com.workproof.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Friendly page for "you are signed in but not allowed to do this" (HTTP 403). */
@WebServlet(name = "AccessDeniedServlet", urlPatterns = {"/access-denied"})
public class AccessDeniedServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute("errorTitle", "Access denied");
        if (request.getAttribute("errorMessage") == null) {
            request.setAttribute("errorMessage",
                    "Your account does not have permission to open this page. "
                  + "If you believe this is a mistake, please contact your project manager or administrator.");
        }
        render(request, response, "errors/403.jsp");
    }
}

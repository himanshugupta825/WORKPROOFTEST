package com.workproof.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Central error page used by web.xml (error-code 404/500 and exception-type).
 *
 * Java exceptions are configured with a "java.lang.Throwable" mapping, which means the
 * stack trace is logged by Tomcat but NEVER rendered to the visitor - the spec requires
 * error pages that do not leak internals.
 */
@WebServlet(name = "ErrorServlet", urlPatterns = {"/error"})
public class ErrorServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer status = intParam(request, "code");
        Throwable cause = (Throwable) request.getAttribute("javax.servlet.error.exception");

        if (cause != null) {
            log("Unhandled exception reached the error page", cause);
        }

        if (status == null) {
            Object original = request.getAttribute("javax.servlet.error.status_code");
            status = original instanceof Integer ? (Integer) original : 500;
        }

        String title;
        String message;
        switch (status) {
            case 403:
                title = "Access denied";
                message = "You do not have permission to perform that action.";
                break;
            case 404:
                title = "Page not found";
                message = "The page or record you asked for does not exist, "
                        + "or you followed an old link.";
                break;
            case 400:
                title = "Bad request";
                message = "The request could not be understood. Please go back and try again.";
                break;
            case 500:
            default:
                title = "Something went wrong";
                message = "The application ran into an unexpected problem. "
                        + "The technical details are in the Tomcat log "
                        + "(logs/catalina.out) - please share them with your administrator.";
                break;
        }

        response.setStatus(status);
        request.setAttribute("errorCode", status);
        request.setAttribute("errorTitle", title);
        request.setAttribute("errorMessage", message);
        render(request, response, "errors/generic.jsp");
    }
}

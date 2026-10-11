package com.workproof.controller;

import com.workproof.exception.AuthorizationException;
import com.workproof.exception.NotFoundException;
import com.workproof.exception.ValidationException;
import com.workproof.model.User;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Common behaviour for every WorkProof controller.
 *
 * It provides:
 *   - typed access to the logged-in user and to request parameters,
 *   - helpers to render a view or redirect with a flash message (Post/Redirect/Get),
 *   - ONE place that converts service exceptions into the right HTTP response:
 *       ValidationException    -> the form is shown again with the message
 *       AuthorizationException -> 403 page
 *       NotFoundException      -> 404 page
 *       anything else          -> 500 page, details only in the server log
 *
 * Because of this, servlets stay short and readable, which matters for the viva.
 */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* ----------------------------- request helpers ----------------------------- */

    protected User currentUser(HttpServletRequest request) {
        return WebUtil.currentUser(request);
    }

    protected String param(HttpServletRequest request, String name) {
        return WebUtil.param(request, name);
    }

    protected Integer intParam(HttpServletRequest request, String name) {
        return WebUtil.intParam(request, name);
    }

    protected int requiredIntParam(HttpServletRequest request, String name, String label) {
        return WebUtil.requiredIntParam(request, name, label);
    }

    protected LocalDate dateParam(HttpServletRequest request, String name) {
        return WebUtil.dateParam(request, name);
    }

    /** Reads every value of a multi-select (e.g. assignees) as integers. */
    protected List<Integer> intParams(HttpServletRequest request, String name) {
        List<Integer> values = new ArrayList<>();
        String[] raw = request.getParameterValues(name);
        if (raw == null) {
            return values;
        }
        for (String value : raw) {
            String clean = ValidationUtil.trimToNull(value);
            if (clean == null) {
                continue;
            }
            try {
                values.add(Integer.parseInt(clean));
            } catch (NumberFormatException ignored) {
                // A non-numeric checkbox value is simply skipped; validation happens in the service.
            }
        }
        return values;
    }

    protected String clientIp(HttpServletRequest request) {
        return WebUtil.clientIp(request);
    }

    /* ------------------------------- navigation ------------------------------- */

    protected void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        WebUtil.forward(request, response, view);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        WebUtil.redirect(response, request, path);
    }

    /** Redirect with a green banner (classic Post/Redirect/Get so a refresh cannot repost). */
    protected void redirectWithSuccess(HttpServletRequest request, HttpServletResponse response,
                                       String path, String message) throws IOException {
        WebUtil.flashSuccess(request, message);
        redirect(request, response, path);
    }

    protected void redirectWithError(HttpServletRequest request, HttpServletResponse response,
                                     String path, String message) throws IOException {
        WebUtil.flashError(request, message);
        redirect(request, response, path);
    }

    /* --------------------------- error page rendering --------------------------- */

    protected void sendForbidden(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute("errorTitle", "Access denied");
        request.setAttribute("errorMessage", message);
        render(request, response, "errors/403.jsp");
    }

    protected void sendNotFound(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("errorTitle", "Not found");
        request.setAttribute("errorMessage", message);
        render(request, response, "errors/404.jsp");
    }

    protected void sendServerError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        request.setAttribute("errorTitle", "Something went wrong");
        request.setAttribute("errorMessage", message);
        render(request, response, "errors/500.jsp");
    }

    /**
     * Translates a service exception into the proper response.
     *
     * @param silentValidation when true a ValidationException is rethrown so the
     *                         caller can show the form again with the message
     * @return true when the exception was handled (a response was written)
     */
    protected boolean handleServiceException(HttpServletRequest request, HttpServletResponse response,
                                             RuntimeException exception, boolean silentValidation)
            throws ServletException, IOException {

        if (exception instanceof AuthorizationException) {
            sendForbidden(request, response, exception.getMessage());
            return true;
        }
        if (exception instanceof NotFoundException) {
            sendNotFound(request, response, exception.getMessage());
            return true;
        }
        if (exception instanceof com.workproof.exception.ValidationException) {
            if (silentValidation) {
                return false;   // handled by the caller (form redisplay)
            }
            // A missing or malformed parameter on a page request (for example ?id=abc)
            // is the visitor's problem, not a server error.
            forwardToError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "That request could not be understood",
                    exception.getMessage() + " Please go back and try again.");
            return true;
        }
        // Unexpected error: log the stack trace server-side, show a friendly page.
        log("Unexpected error while handling " + request.getRequestURI(), exception);
        sendServerError(request, response, "An unexpected error occurred. "
                + "The details have been written to the Tomcat log.");
        return true;
    }

    /**
     * Safety net for every controller.
     *
     * Servlets read request parameters before their try/catch blocks (for example
     * requiredIntParam at the top of doGet). A malformed or missing parameter, a
     * missing record or a permission problem raised there would otherwise escape to
     * the container and become an HTTP 500. Catching the three known business
     * exception types here turns them into the correct 400/403/404 pages, and a real
     * bug still produces a 500 with the stack trace only in the Tomcat log.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            super.service(request, response);
        } catch (AuthorizationException e) {
            if (response.isCommitted()) {
                throw e;
            }
            sendForbidden(request, response, e.getMessage());
        } catch (NotFoundException e) {
            if (response.isCommitted()) {
                throw e;
            }
            sendNotFound(request, response, e.getMessage());
        } catch (ValidationException e) {
            if (response.isCommitted()) {
                throw e;
            }
            forwardToError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "That request could not be understood",
                    e.getMessage() + " Please go back and try again.");
        }
    }

    /** Convenience: renders a JSP through the error page machinery. */
    protected void forwardToError(HttpServletRequest request, HttpServletResponse response,
                                  int status, String title, String message)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorTitle", title);
        request.setAttribute("errorMessage", message);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/errors/generic.jsp");
        dispatcher.forward(request, response);
    }
}

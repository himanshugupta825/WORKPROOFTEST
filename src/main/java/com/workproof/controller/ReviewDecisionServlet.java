package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.User;
import com.workproof.service.ReviewService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Records an approval or a rejection.
 *
 * This is a POST-only endpoint protected by the CSRF filter, so a decision can never
 * be triggered by an image tag or a link in an e-mail.
 */
@WebServlet(name = "ReviewDecisionServlet", urlPatterns = {"/reviews/decision"})
public class ReviewDecisionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User reviewer = currentUser(request);
        Integer submissionId = intParam(request, "submissionId");
        String decision = param(request, "decision");
        String comments = param(request, "comments");

        if (submissionId == null) {
            redirectWithError(request, response, "/reviews", "Submission id is missing.");
            return;
        }
        String backTo = "/reviews/view?id=" + submissionId;

        try {
            reviewService.decide(reviewer, submissionId, decision, comments, clientIp(request));
            String label = "APPROVED".equalsIgnoreCase(decision) ? "approved" : "rejected";
            redirectWithSuccess(request, response, backTo,
                    "Submission " + label + ". The team member has been notified" + ".");
        } catch (ValidationException e) {
            redirectWithError(request, response, backTo, e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

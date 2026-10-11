package com.workproof.controller;

import com.workproof.model.ProofSubmission;
import com.workproof.model.User;
import com.workproof.service.ReviewService;
import com.workproof.service.SubmissionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Details of one evidence submission: description, files, review decision and the
 * complete version history of the task.
 */
@WebServlet(name = "SubmissionViewServlet", urlPatterns = {"/submissions/view"})
public class SubmissionViewServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final SubmissionService submissionService = new SubmissionService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int submissionId = requiredIntParam(request, "id", "Submission");

        try {
            ProofSubmission submission = submissionService.getForViewer(user, submissionId);
            request.setAttribute("submission", submission);
            request.setAttribute("history", submissionService.historyForTask(user,
                    submissionService.getTaskForSubmission(submission)));
            request.setAttribute("canDecide", reviewService.canDecide(user, submission));
            request.setAttribute("selfReviewBlocked", reviewService.isSelfReview(submission, user));
            request.setAttribute("selfReviewAllowed", reviewService.selfReviewAllowed());
            render(request, response, "submissions/view.jsp");
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

package com.workproof.controller;

import com.workproof.model.ProofSubmission;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.ReviewService;
import com.workproof.service.SubmissionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Review screen: the evidence and everything the reviewer needs to make a decision -
 * task requirements, files, and the previous versions with their feedback.
 */
@WebServlet(name = "ReviewViewServlet", urlPatterns = {"/reviews/view"})
public class ReviewViewServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ReviewService reviewService = new ReviewService();
    private final SubmissionService submissionService = new SubmissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int submissionId = requiredIntParam(request, "id", "Submission");

        try {
            ProofSubmission submission = reviewService.getForReview(user, submissionId);
            Task task = submissionService.getTaskForSubmission(submission);

            request.setAttribute("submission", submission);
            request.setAttribute("task", task);
            request.setAttribute("history", submissionService.historyForTask(user, task));
            request.setAttribute("canDecide", reviewService.canDecide(user, submission));
            request.setAttribute("selfReviewBlocked", reviewService.isSelfReview(submission, user));
            request.setAttribute("selfReviewAllowed", reviewService.selfReviewAllowed());
            render(request, response, "reviews/view.jsp");
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

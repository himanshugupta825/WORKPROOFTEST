package com.workproof.controller;

import com.workproof.exception.AuthorizationException;
import com.workproof.model.PagedResult;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Review;
import com.workproof.model.User;
import com.workproof.service.Permissions;
import com.workproof.service.ReviewService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Verification queue (Module 6): the evidence waiting for a decision, plus the
 * reviewer's own decision history.
 *
 * Only reviewers, managers and administrators may open this page - a team member is
 * sent to the access-denied page even if they type the URL by hand.
 */
@WebServlet(name = "ReviewListServlet", urlPatterns = {"/reviews"})
public class ReviewListServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 10;

    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);

        try {
            Permissions.requireRole(user, com.workproof.model.enums.RoleName.REVIEWER,
                    com.workproof.model.enums.RoleName.MANAGER, com.workproof.model.enums.RoleName.ADMIN);

            String search = param(request, "search");
            String decision = param(request, "decision");
            int page = WebUtil.pageParam(request);

            PagedResult<ProofSubmission> pending = reviewService.pendingFor(user, search, page, PAGE_SIZE);
            PagedResult<Review> history = reviewService.historyFor(user, decision, null, 1, 10);

            request.setAttribute("pendingPage", pending);
            request.setAttribute("historyPage", history);
            request.setAttribute("search", search);
            request.setAttribute("decision", ValidationUtil.trimToNull(decision) == null ? "ALL" : decision);
            request.setAttribute("pendingCount", reviewService.countPendingFor(user));
            request.setAttribute("selfReviewAllowed", reviewService.selfReviewAllowed());
            render(request, response, "reviews/list.jsp");

        } catch (AuthorizationException e) {
            sendForbidden(request, response, e.getMessage());
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

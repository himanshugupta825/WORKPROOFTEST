package com.workproof.controller;

import com.workproof.model.PagedResult;
import com.workproof.model.Project;
import com.workproof.model.ProofSubmission;
import com.workproof.model.User;
import com.workproof.service.ProjectService;
import com.workproof.service.SubmissionService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * "My evidence" listing: every submission of the logged-in user together with the
 * reviewer's decision, so a member can follow up on feedback.
 */
@WebServlet(name = "SubmissionListServlet", urlPatterns = {"/submissions"})
public class SubmissionListServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 10;

    private final SubmissionService submissionService = new SubmissionService();
    private final ProjectService projectService = new ProjectService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String status = param(request, "status");
        Integer projectId = intParam(request, "projectId");
        LocalDate from = dateParam(request, "from");
        LocalDate to = dateParam(request, "to");
        int page = WebUtil.pageParam(request);

        PagedResult<ProofSubmission> submissions = submissionService.listMine(user, status, projectId,
                from, to, page, PAGE_SIZE);

        List<Project> projects = projectService.findProjectsForUser(user);

        request.setAttribute("submissionPage", submissions);
        request.setAttribute("projects", projects);
        request.setAttribute("statuses", submissionService.allStatuses());
        request.setAttribute("status", ValidationUtil.trimToNull(status) == null ? "ALL" : status);
        request.setAttribute("selectedProjectId", projectId);
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.setAttribute("maxFileSize", submissionService.maxFileSizeLabel());
        // Summary counters for the cards above the table (real COUNT queries, not page maths).
        request.setAttribute("pendingCount", submissionService.countForSubmitter(user.getId(), "PENDING"));
        request.setAttribute("approvedCount", submissionService.countForSubmitter(user.getId(), "APPROVED"));
        request.setAttribute("rejectedCount", submissionService.countForSubmitter(user.getId(), "REJECTED"));
        render(request, response, "submissions/list.jsp");
    }
}

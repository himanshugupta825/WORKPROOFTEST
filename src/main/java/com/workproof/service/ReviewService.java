package com.workproof.service;

import com.workproof.dao.ProjectDao;
import com.workproof.dao.ReviewDao;
import com.workproof.dao.SubmissionDao;
import com.workproof.dao.TaskDao;
import com.workproof.exception.AuthorizationException;
import com.workproof.exception.NotFoundException;
import com.workproof.exception.ValidationException;
import com.workproof.model.PagedResult;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Review;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.model.enums.ProjectRole;
import com.workproof.model.enums.ReviewDecision;
import com.workproof.model.enums.SubmissionStatus;
import com.workproof.model.enums.TaskStatus;
import com.workproof.util.AppConfig;
import com.workproof.util.ValidationUtil;

import java.util.List;

/**
 * Verification and review workflow (Module 6).
 *
 * Rules enforced here:
 *   - only reviewers/managers of the project (or an administrator) may decide,
 *   - the "no self-approval" rule: a user may not review their own submission unless
 *     the documented project rule workproof.review.allowSelfReview=true is set,
 *   - a rejection always requires written feedback (minimum length enforced),
 *   - a submission can only be decided once,
 *   - approving marks the task APPROVED, rejecting returns it to work and allows a
 *     new version to be submitted later.
 */
public class ReviewService {

    /** Minimum length of the feedback comment on a rejection. */
    private static final int MIN_REJECTION_COMMENT = 10;

    private final ReviewDao reviewDao = new ReviewDao();
    private final SubmissionDao submissionDao = new SubmissionDao();
    private final TaskDao taskDao = new TaskDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final ActivityService activityService = new ActivityService();
    private final NotificationService notificationService = new NotificationService();
    private final ProjectService projectService = new ProjectService();

    public boolean selfReviewAllowed() {
        return AppConfig.allowSelfReview();
    }

    /* =============================== queries =============================== */

    /**
     * The review queue of the logged-in user.
     * Admins see everything; managers and reviewers see the projects they are attached to.
     */
    public PagedResult<ProofSubmission> pendingFor(User viewer, String search, int page, int pageSize) {
        if (viewer.isAdmin()) {
            return submissionDao.searchPending(null, null, search, page, pageSize);
        }
        if (viewer.isReviewer()) {
            PagedResult<ProofSubmission> asReviewer =
                    submissionDao.searchPending(viewer.getId(), ProjectRole.REVIEWER.name(), search, page, pageSize);
            // A mentor can also be a project manager elsewhere, so merge both queues.
            PagedResult<ProofSubmission> asManager =
                    submissionDao.searchPending(viewer.getId(), ProjectRole.MANAGER.name(), search, page, pageSize);
            return merge(asReviewer, asManager, page, pageSize);
        }
        // Team leads (global MANAGER role) only see their own projects.
        return submissionDao.searchPending(viewer.getId(), ProjectRole.MANAGER.name(), search, page, pageSize);
    }

    /** Review history: all decisions (admin) or the decisions of this reviewer. */
    public PagedResult<Review> historyFor(User viewer, String decision, String search, int page, int pageSize) {
        Integer reviewerFilter = viewer.isAdmin() ? null : viewer.getId();
        return reviewDao.search(reviewerFilter, decision, search, page, pageSize);
    }

    public List<Review> recentDecisions(User viewer, int limit) {
        return viewer.isAdmin() ? reviewDao.findRecent(limit) : reviewDao.findRecentByReviewer(viewer.getId(), limit);
    }

    /** A submission ready for the review screen, with the permission check applied. */
    public ProofSubmission getForReview(User viewer, int submissionId) {
        ProofSubmission submission = submissionDao.findById(submissionId);
        if (submission == null) {
            throw new NotFoundException("Submission #" + submissionId + " was not found.");
        }
        String roleInProject = viewer.isAdmin() ? "ADMIN"
                : projectDao.findProjectRole(submission.getProjectId(), viewer.getId());
        Permissions.requireReviewPermission(viewer, roleInProject, submission.getProjectName());
        return submission;
    }

    /** true when the viewer may press Approve/Reject on this submission. */
    public boolean canDecide(User viewer, ProofSubmission submission) {
        if (submission == null || !submission.isPending()) {
            return false;
        }
        String roleInProject = viewer.isAdmin() ? "ADMIN"
                : projectDao.findProjectRole(submission.getProjectId(), viewer.getId());
        if (!Permissions.canReviewInProject(viewer, roleInProject)) {
            return false;
        }
        return !isSelfReview(submission, viewer);
    }

    /** The documented self-approval rule. */
    public boolean isSelfReview(ProofSubmission submission, User viewer) {
        if (submission.getSubmittedBy() != viewer.getId()) {
            return false;
        }
        return !AppConfig.allowSelfReview();
    }

    public int countPendingFor(User viewer) {
        if (viewer.isAdmin()) {
            return submissionDao.countByStatus(SubmissionStatus.PENDING.name());
        }
        return submissionDao.countPendingForReviewer(viewer.getId(), ProjectRole.REVIEWER.name())
                + submissionDao.countPendingForReviewer(viewer.getId(), ProjectRole.MANAGER.name());
    }

    /* ================================ decide ================================ */

    /**
     * Records an approval or rejection.
     *
     * @param decisionValue APPROVED or REJECTED
     * @param comments      mandatory for a rejection
     */
    public void decide(User reviewer, int submissionId, String decisionValue, String comments, String ip) {

        ProofSubmission submission = submissionDao.findById(submissionId);
        if (submission == null) {
            throw new NotFoundException("Submission #" + submissionId + " was not found.");
        }
        Task task = taskDao.findById(submission.getTaskId());
        if (task == null) {
            throw new NotFoundException("The task of this submission no longer exists.");
        }

        // ---- permission ----
        String roleInProject = reviewer.isAdmin() ? "ADMIN"
                : projectDao.findProjectRole(submission.getProjectId(), reviewer.getId());
        Permissions.requireReviewPermission(reviewer, roleInProject, submission.getProjectName());

        // ---- no self-approval (documented project rule) ----
        if (submission.getSubmittedBy() == reviewer.getId() && !AppConfig.allowSelfReview()) {
            throw new AuthorizationException("You cannot review your own submission. "
                    + "Please ask another reviewer or the project manager to verify it. "
                    + "(A team lead may enable workproof.review.allowSelfReview=true if the "
                    + "college explicitly permits self-review.)");
        }

        // ---- state + input validation ----
        if (!submission.isPending()) {
            throw new ValidationException("This submission was already reviewed ("
                    + submission.getSubmissionStatus().getLabel() + "). Reload the page to see the decision.");
        }

        ReviewDecision decision = ReviewDecision.from(decisionValue);
        comments = ValidationUtil.trimToNull(comments);

        if (decision == ReviewDecision.REJECTED) {
            if (comments == null || comments.length() < MIN_REJECTION_COMMENT) {
                throw new ValidationException("Please explain why the evidence is rejected "
                        + "(at least " + MIN_REJECTION_COMMENT + " characters). The team member needs "
                        + "actionable feedback before resubmitting.");
            }
        } else {
            comments = comments == null ? "Approved. The submitted evidence matches the task requirements."
                    : comments;
        }

        // ---- persist: review row + submission status + task status in one transaction ----
        Review review = new Review();
        review.setSubmissionId(submissionId);
        review.setReviewerId(reviewer.getId());
        review.setDecision(decision.name());
        review.setComments(comments);

        String newTaskStatus = decision == ReviewDecision.APPROVED
                ? TaskStatus.APPROVED.name()
                : TaskStatus.REJECTED.name();

        reviewDao.submitDecision(review, decision.name(), newTaskStatus);

        // ---- audit + notify ----
        activityService.log(reviewer,
                decision == ReviewDecision.APPROVED ? ActivityService.REVIEW_APPROVED : ActivityService.REVIEW_REJECTED,
                "SUBMISSION", submissionId,
                decision.getLabel() + " submission v" + submission.getVersion() + " for task '"
                        + task.getTitle() + "'" + (comments.isEmpty() ? "" : " - " + comments), ip);

        notificationService.notifyReviewDecision(submission, task, reviewer, decision.name(), comments);
    }

    /* ================================ helpers ================================ */

    /**
     * Merges two paged results (reviewer queue + manager queue) without duplicates.
     * Pagination is applied again after merging so the counters stay consistent.
     */
    private PagedResult<ProofSubmission> merge(PagedResult<ProofSubmission> first,
                                               PagedResult<ProofSubmission> second, int page, int pageSize) {
        java.util.LinkedHashMap<Integer, ProofSubmission> merged = new java.util.LinkedHashMap<>();
        for (ProofSubmission s : first.getItems()) {
            merged.put(s.getId(), s);
        }
        for (ProofSubmission s : second.getItems()) {
            merged.putIfAbsent(s.getId(), s);
        }
        List<ProofSubmission> all = new java.util.ArrayList<>(merged.values());
        all.sort(java.util.Comparator.comparing(ProofSubmission::getSubmittedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

        int total = first.getTotalItems() + second.getTotalItems() - duplicateCount(first, second);
        int from = Math.min((page - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PagedResult<>(new java.util.ArrayList<>(all.subList(from, to)), total, page, pageSize);
    }

    private int duplicateCount(PagedResult<ProofSubmission> first, PagedResult<ProofSubmission> second) {
        int duplicates = 0;
        for (ProofSubmission s : first.getItems()) {
            for (ProofSubmission t : second.getItems()) {
                if (s.getId() == t.getId()) {
                    duplicates++;
                }
            }
        }
        return duplicates;
    }

    /** Managers/reviewers of a project, used by the "who must still review" hint on screen. */
    public List<Integer> reviewerIdsForProject(int projectId) {
        return projectService.findReviewerIds(projectId);
    }
}

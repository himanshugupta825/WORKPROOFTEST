package com.workproof.model;

import com.workproof.model.enums.ReviewDecision;

import java.time.LocalDateTime;

/** Row of `reviews` - the reviewer's decision and comments. */
public class Review {

    private int id;
    private int submissionId;
    private int reviewerId;
    private String decision;
    private String comments;
    private LocalDateTime reviewedAt;

    /* joined */
    private String reviewerName;
    private String submissionTitle;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }

    public int getReviewerId() { return reviewerId; }
    public void setReviewerId(int reviewerId) { this.reviewerId = reviewerId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getSubmissionTitle() { return submissionTitle; }
    public void setSubmissionTitle(String submissionTitle) { this.submissionTitle = submissionTitle; }

    public ReviewDecision getReviewDecision() {
        return ReviewDecision.from(decision);
    }

    public boolean isApproved() {
        return getReviewDecision() == ReviewDecision.APPROVED;
    }
}

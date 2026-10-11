package com.workproof.dao;

import com.workproof.model.PagedResult;
import com.workproof.model.Review;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for `reviews`.
 *
 * A review is inseparable from its consequences, therefore
 * {@link #submitDecision} writes the review row, the new submission status and the
 * new task status inside a single transaction.
 */
public class ReviewDao extends BaseDao {

    private static final String BASE_SELECT =
            "SELECT r.id, r.submission_id, r.reviewer_id, r.decision, r.comments, r.reviewed_at, "
          + "       u.full_name AS reviewer_name, s.title AS submission_title "
          + "FROM reviews r "
          + "JOIN users u ON u.id = r.reviewer_id "
          + "JOIN proof_submissions s ON s.id = r.submission_id ";

    private final RowMapper<Review> mapper = (ResultSet rs) -> {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setSubmissionId(rs.getInt("submission_id"));
        r.setReviewerId(rs.getInt("reviewer_id"));
        r.setDecision(rs.getString("decision"));
        r.setComments(rs.getString("comments"));
        r.setReviewedAt(toLocalDateTime(rs.getTimestamp("reviewed_at")));
        r.setReviewerName(rs.getString("reviewer_name"));
        r.setSubmissionTitle(rs.getString("submission_title"));
        return r;
    };

    public Review findBySubmission(int submissionId) {
        return one(BASE_SELECT + "WHERE r.submission_id = ?", mapper, submissionId);
    }

    public boolean existsForSubmission(int submissionId) {
        return count("SELECT COUNT(*) FROM reviews WHERE submission_id = ?", submissionId) > 0;
    }

    /**
     * Review history.
     *
     * @param reviewerId when set, only decisions made by this reviewer
     * @param decision   APPROVED / REJECTED, or null for both
     */
    public PagedResult<Review> search(Integer reviewerId, String decision, String search, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (reviewerId != null) {
            where.append(" AND r.reviewer_id = ? ");
            params.add(reviewerId);
        }
        if (decision != null && !decision.isEmpty() && !"ALL".equalsIgnoreCase(decision)) {
            where.append(" AND r.decision = ? ");
            params.add(decision);
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (s.title LIKE ? OR r.comments LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }

        int total = count("SELECT COUNT(*) FROM reviews r JOIN proof_submissions s ON s.id = r.submission_id "
                        + "JOIN users u ON u.id = r.reviewer_id " + where, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<Review> reviews = list(BASE_SELECT + where + " ORDER BY r.reviewed_at DESC LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        return new PagedResult<>(reviews, total, page, pageSize);
    }

    /** Recent decisions shown on the reviewer dashboard. */
    public List<Review> findRecentByReviewer(int reviewerId, int limit) {
        return list(BASE_SELECT + "WHERE r.reviewer_id = ? ORDER BY r.reviewed_at DESC LIMIT ?",
                mapper, reviewerId, limit);
    }

    public List<Review> findRecent(int limit) {
        return list(BASE_SELECT + "ORDER BY r.reviewed_at DESC LIMIT ?", mapper, limit);
    }

    public int countByReviewerAndDecision(int reviewerId, String decision) {
        return count("SELECT COUNT(*) FROM reviews WHERE reviewer_id = ? AND decision = ?", reviewerId, decision);
    }

    public int countByDecision(String decision) {
        return count("SELECT COUNT(*) FROM reviews WHERE decision = ?", decision);
    }

    /* ================================ writes ================================ */

    /**
     * Records a reviewer decision and applies its consequences atomically:
     *   1. INSERT INTO reviews
     *   2. UPDATE proof_submissions.status  (APPROVED / REJECTED)
     *   3. UPDATE tasks.status              (APPROVED / REJECTED)
     *
     * @return the generated review id
     */
    public int submitDecision(final Review review, final String newSubmissionStatus, final String newTaskStatus) {
        return tx((Connection c) -> {
            int reviewId = insert(c, "INSERT INTO reviews (submission_id, reviewer_id, decision, comments) "
                                   + "VALUES (?, ?, ?, ?)",
                    review.getSubmissionId(), review.getReviewerId(), review.getDecision(), review.getComments());

            update(c, "UPDATE proof_submissions SET status = ? WHERE id = ?",
                    newSubmissionStatus, review.getSubmissionId());

            update(c, "UPDATE tasks SET status = ?, completed_at = CASE WHEN ? = 'APPROVED' THEN ? ELSE NULL END "
                    + "WHERE id = (SELECT task_id FROM (SELECT task_id FROM proof_submissions WHERE id = ?) AS x)",
                    newTaskStatus, newTaskStatus, java.time.LocalDateTime.now(), review.getSubmissionId());

            return reviewId;
        });
    }
}

package com.workproof.dao;

import com.workproof.model.PagedResult;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Review;
import com.workproof.model.SubmissionFile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for `proof_submissions` and `submission_files`.
 *
 * The most important method is {@link #createWithFilesAndTaskStatus}: it inserts the
 * submission, its files and the new task status inside one transaction, so the
 * database can never contain a submission whose file rows are missing.
 */
public class SubmissionDao extends BaseDao {

    private static final String BASE_SELECT =
            "SELECT s.id, s.task_id, s.submitted_by, s.title, s.description, s.status, s.version, "
          + "       s.submitted_at, s.updated_at, u.full_name AS submitter_name, "
          + "       t.title AS task_title, t.status AS task_status, t.project_id, p.name AS project_name, "
          + "       r.id AS review_id, r.reviewer_id, r.decision, r.comments, r.reviewed_at, "
          + "       ru.full_name AS reviewer_name "
          + "FROM proof_submissions s "
          + "JOIN tasks t ON t.id = s.task_id "
          + "JOIN projects p ON p.id = t.project_id "
          + "JOIN users u ON u.id = s.submitted_by "
          + "LEFT JOIN reviews r ON r.submission_id = s.id "
          + "LEFT JOIN users ru ON ru.id = r.reviewer_id ";

    private final RowMapper<ProofSubmission> mapper = (ResultSet rs) -> {
        ProofSubmission s = new ProofSubmission();
        s.setId(rs.getInt("id"));
        s.setTaskId(rs.getInt("task_id"));
        s.setSubmittedBy(rs.getInt("submitted_by"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setStatus(rs.getString("status"));
        s.setVersion(rs.getInt("version"));
        s.setSubmittedAt(toLocalDateTime(rs.getTimestamp("submitted_at")));
        s.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        s.setSubmitterName(rs.getString("submitter_name"));
        s.setTaskTitle(rs.getString("task_title"));
        s.setTaskStatus(rs.getString("task_status"));
        s.setProjectId(rs.getInt("project_id"));
        s.setProjectName(rs.getString("project_name"));

        int reviewId = rs.getInt("review_id");
        if (!rs.wasNull()) {
            Review review = new Review();
            review.setId(reviewId);
            review.setSubmissionId(s.getId());
            review.setReviewerId(rs.getInt("reviewer_id"));
            review.setDecision(rs.getString("decision"));
            review.setComments(rs.getString("comments"));
            review.setReviewedAt(toLocalDateTime(rs.getTimestamp("reviewed_at")));
            review.setReviewerName(rs.getString("reviewer_name"));
            s.setReview(review);
        }
        return s;
    };

    private final RowMapper<SubmissionFile> fileMapper = (ResultSet rs) -> {
        SubmissionFile f = new SubmissionFile();
        f.setId(rs.getInt("id"));
        f.setSubmissionId(rs.getInt("submission_id"));
        f.setOriginalName(rs.getString("original_name"));
        f.setStoredName(rs.getString("stored_name"));
        f.setContentType(rs.getString("content_type"));
        f.setFileSize(rs.getLong("file_size"));
        f.setSha256(rs.getString("sha256"));
        f.setUploadedAt(toLocalDateTime(rs.getTimestamp("uploaded_at")));
        return f;
    };

    private static final String FILE_SELECT =
            "SELECT id, submission_id, original_name, stored_name, content_type, file_size, sha256, uploaded_at "
          + "FROM submission_files ";

    /* ================================ reads ================================ */

    public ProofSubmission findById(int id) {
        ProofSubmission submission = one(BASE_SELECT + "WHERE s.id = ?", mapper, id);
        if (submission != null) {
            submission.setFiles(findFiles(id));
        }
        return submission;
    }

    /** Full submission history of a task, newest first (version N, N-1, ...). */
    public List<ProofSubmission> findByTask(int taskId) {
        List<ProofSubmission> submissions = list(BASE_SELECT + "WHERE s.task_id = ? ORDER BY s.version DESC",
                mapper, taskId);
        for (ProofSubmission s : submissions) {
            s.setFiles(findFiles(s.getId()));
        }
        return submissions;
    }

    public ProofSubmission findLatestByTask(int taskId) {
        ProofSubmission submission = one(BASE_SELECT + "WHERE s.task_id = ? ORDER BY s.version DESC LIMIT 1",
                mapper, taskId);
        if (submission != null) {
            submission.setFiles(findFiles(submission.getId()));
        }
        return submission;
    }

    public ProofSubmission findPendingByTask(int taskId) {
        return one(BASE_SELECT + "WHERE s.task_id = ? AND s.status = 'PENDING' ORDER BY s.version DESC LIMIT 1",
                mapper, taskId);
    }

    public List<SubmissionFile> findFiles(int submissionId) {
        return list(FILE_SELECT + "WHERE submission_id = ? ORDER BY id", fileMapper, submissionId);
    }

    public SubmissionFile findFile(int fileId) {
        return one(FILE_SELECT + "WHERE id = ?", fileMapper, fileId);
    }

    /** Counts used when a new version number is calculated: max(version) + 1. */
    public int nextVersion(int taskId) {
        return count("SELECT COALESCE(MAX(version), 0) + 1 FROM proof_submissions WHERE task_id = ?", taskId);
    }

    /**
     * Submissions waiting for a decision.
     *
     * @param reviewerUserId when not null, restricted to projects where this user holds
     *                       {@code projectRole} (REVIEWER for mentors, MANAGER for team leads)
     */
    public PagedResult<ProofSubmission> searchPending(Integer reviewerUserId, String projectRole, String search,
                                                      int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE s.status = 'PENDING' ");
        List<Object> params = new ArrayList<>();

        if (reviewerUserId != null && projectRole != null) {
            where.append(" AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = t.project_id "
                       + "             AND pm.user_id = ? AND pm.role_in_project = ?) ");
            params.add(reviewerUserId);
            params.add(projectRole);
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (s.title LIKE ? OR t.title LIKE ? OR p.name LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        int total = count("SELECT COUNT(*) FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
                        + "JOIN projects p ON p.id = t.project_id " + where, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<ProofSubmission> items = list(BASE_SELECT + where + " ORDER BY s.submitted_at ASC LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        attachFiles(items);
        return new PagedResult<>(items, total, page, pageSize);
    }

    /**
     * General purpose submission search used by the reports module.
     *
     * @param status  PENDING / APPROVED / REJECTED / SUPERSEDED, or null for all
     * @param dateFrom inclusive lower bound on submitted_at (nullable)
     * @param dateTo   inclusive upper bound on submitted_at (nullable)
     */
    public PagedResult<ProofSubmission> search(Integer submitterId, Integer projectId, String status,
                                               java.time.LocalDate dateFrom, java.time.LocalDate dateTo,
                                               String search, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (submitterId != null) {
            where.append(" AND s.submitted_by = ? ");
            params.add(submitterId);
        }
        if (projectId != null) {
            where.append(" AND t.project_id = ? ");
            params.add(projectId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND s.status = ? ");
            params.add(status);
        }
        if (dateFrom != null) {
            where.append(" AND s.submitted_at >= ? ");
            params.add(dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            where.append(" AND s.submitted_at < ? ");
            params.add(dateTo.plusDays(1).atStartOfDay());
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (s.title LIKE ? OR t.title LIKE ? OR u.full_name LIKE ?) ");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        int total = count("SELECT COUNT(*) FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
                        + "JOIN projects p ON p.id = t.project_id JOIN users u ON u.id = s.submitted_by " + where,
                params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<ProofSubmission> items = list(BASE_SELECT + where + " ORDER BY s.submitted_at DESC LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        attachFiles(items);
        return new PagedResult<>(items, total, page, pageSize);
    }

    /** Unpaginated list used for CSV export, protected by a hard row cap. */
    public List<ProofSubmission> findForExport(Integer projectId, String status,
                                               java.time.LocalDate dateFrom, java.time.LocalDate dateTo,
                                               int maxRows) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (projectId != null) {
            where.append(" AND t.project_id = ? ");
            params.add(projectId);
        }
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append(" AND s.status = ? ");
            params.add(status);
        }
        if (dateFrom != null) {
            where.append(" AND s.submitted_at >= ? ");
            params.add(dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            where.append(" AND s.submitted_at < ? ");
            params.add(dateTo.plusDays(1).atStartOfDay());
        }
        params.add(maxRows);
        return list(BASE_SELECT + where + " ORDER BY s.submitted_at DESC LIMIT ?", mapper, params.toArray());
    }

    /* ================================ counts ================================ */

    public int countByStatus(String status) {
        return count("SELECT COUNT(*) FROM proof_submissions WHERE status = ?", status);
    }

    public int countByStatusForSubmitter(int submitterId, String status) {
        return count("SELECT COUNT(*) FROM proof_submissions WHERE submitted_by = ? AND status = ?",
                submitterId, status);
    }

    public int countAllForSubmitter(int submitterId) {
        return count("SELECT COUNT(*) FROM proof_submissions WHERE submitted_by = ?", submitterId);
    }

    public int countPendingForReviewer(int reviewerUserId, String projectRole) {
        return count("SELECT COUNT(*) FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
                   + "WHERE s.status = 'PENDING' AND EXISTS (SELECT 1 FROM project_members pm "
                   + "WHERE pm.project_id = t.project_id AND pm.user_id = ? AND pm.role_in_project = ?)",
                reviewerUserId, projectRole);
    }

    /**
     * Reviewers/managers see the verification backlog of their own projects only.
     * The placeholder order in the SQL is: first the statuses, then the user id.
     */
    public int countInStatusesForReviewerScope(int reviewerUserId, List<String> statuses) {
        String placeholders = String.join(",", Collections.nCopies(statuses.size(), "?"));
        List<Object> params = new ArrayList<>(statuses);
        params.add(reviewerUserId);
        return count("SELECT COUNT(*) FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
                   + "WHERE s.status IN (" + placeholders + ") "
                   + "AND EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = t.project_id "
                   + "            AND pm.user_id = ? AND pm.role_in_project IN ('REVIEWER', 'MANAGER'))",
                params.toArray());
    }

    /** Counts per status for a project - used on the project details page. */
    public Map<String, Integer> countGroupedByStatusForProject(int projectId) {
        Map<String, Integer> result = new HashMap<>();
        list("SELECT s.status, COUNT(*) AS total FROM proof_submissions s JOIN tasks t ON t.id = s.task_id "
           + "WHERE t.project_id = ? GROUP BY s.status", (ResultSet rs) -> {
            result.put(rs.getString("status"), rs.getInt("total"));
            return null;
        }, projectId);
        return result;
    }

    /* ================================ writes ================================ */

    /**
     * Inserts a submission with its files and moves the task to the new status in a
     * single transaction.
     *
     * @param newTaskStatus usually SUBMITTED
     * @return the generated submission id
     */
    public int createWithFilesAndTaskStatus(final ProofSubmission submission, final List<SubmissionFile> files,
                                            final String newTaskStatus) {
        return tx((Connection c) -> {
            // An earlier version that is still waiting for a reviewer is now obsolete:
            // mark it SUPERSEDED so the review queue only ever shows the latest version
            // of a task. Versions that were already APPROVED or REJECTED keep their
            // decision, because the reviewer's history must not be rewritten.
            update(c, "UPDATE proof_submissions SET status = 'SUPERSEDED' "
                    + "WHERE task_id = ? AND status = 'PENDING'", submission.getTaskId());

            int submissionId = insert(c,
                    "INSERT INTO proof_submissions (task_id, submitted_by, title, description, status, version) "
                  + "VALUES (?, ?, ?, ?, 'PENDING', ?)",
                    submission.getTaskId(), submission.getSubmittedBy(), submission.getTitle(),
                    submission.getDescription(), submission.getVersion());

            if (files != null) {
                for (SubmissionFile file : files) {
                    insert(c, "INSERT INTO submission_files (submission_id, original_name, stored_name, "
                            + "content_type, file_size, sha256) VALUES (?, ?, ?, ?, ?, ?)",
                            submissionId, file.getOriginalName(), file.getStoredName(), file.getContentType(),
                            file.getFileSize(), file.getSha256());
                }
            }

            update(c, "UPDATE tasks SET status = ?, completed_at = NULL WHERE id = ?",
                    newTaskStatus, submission.getTaskId());
            return submissionId;
        });
    }

    public int updateStatus(int submissionId, String status) {
        return update("UPDATE proof_submissions SET status = ? WHERE id = ?", status, submissionId);
    }

    public int updateStatus(Connection c, int submissionId, String status) throws SQLException {
        return update(c, "UPDATE proof_submissions SET status = ? WHERE id = ?", status, submissionId);
    }

    /** Used when a submission is deleted together with its files (admin cleanup). */
    public int delete(int submissionId) {
        return update("DELETE FROM proof_submissions WHERE id = ?", submissionId);
    }

    /* ============================== enrichment ============================== */

    private void attachFiles(List<ProofSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(submissions.size(), "?"));
        Object[] ids = submissions.stream().map(ProofSubmission::getId).toArray();

        Map<Integer, List<SubmissionFile>> bySubmission = new HashMap<>();
        list(FILE_SELECT + "WHERE submission_id IN (" + placeholders + ") ORDER BY id", (ResultSet rs) -> {
            SubmissionFile f = fileMapper.map(rs);
            bySubmission.computeIfAbsent(f.getSubmissionId(), k -> new ArrayList<>()).add(f);
            return null;
        }, ids);

        for (ProofSubmission s : submissions) {
            s.setFiles(bySubmission.getOrDefault(s.getId(), new ArrayList<>()));
        }
    }
}

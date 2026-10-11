package com.workproof.service;

import com.workproof.dao.ProjectDao;
import com.workproof.dao.SubmissionDao;
import com.workproof.dao.TaskDao;
import com.workproof.exception.NotFoundException;
import com.workproof.exception.ValidationException;
import com.workproof.model.PagedResult;
import com.workproof.model.Project;
import com.workproof.model.ProofSubmission;
import com.workproof.model.SubmissionFile;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.model.enums.SubmissionStatus;
import com.workproof.model.enums.TaskStatus;
import com.workproof.util.AppConfig;
import com.workproof.util.FileStorageUtil;
import com.workproof.util.ValidationUtil;

import javax.servlet.http.Part;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Proof-of-work submission (Module 5).
 *
 * Workflow implemented here:
 *   1. only an assignee (or the project manager/admin) may submit evidence,
 *   2. the task must not already have evidence waiting for a decision,
 *   3. files are validated and stored OUTSIDE the web root with generated names,
 *   4. the submission row, its file metadata and the new task status are written in
 *      one database transaction,
 *   5. every attempt is kept, so a rejected task keeps its full history.
 */
public class SubmissionService {

    /** Maximum number of files that may be attached to a single submission. */
    public static final int MAX_FILES_PER_SUBMISSION = 5;

    private final SubmissionDao submissionDao = new SubmissionDao();
    private final TaskDao taskDao = new TaskDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final ActivityService activityService = new ActivityService();
    private final NotificationService notificationService = new NotificationService();
    private final ProjectService projectService = new ProjectService();

    public String acceptedFileTypes() {
        return FileStorageUtil.acceptedExtensionsForInput();
    }

    public long maxFileSizeBytes() {
        return AppConfig.uploadMaxFileSizeBytes();
    }

    public String maxFileSizeLabel() {
        return FileStorageUtil.humanReadableSize(AppConfig.uploadMaxFileSizeBytes());
    }

    /* =============================== queries =============================== */

    /** One submission with an access check for the viewer (submitter, project member or admin). */
    public ProofSubmission getForViewer(User viewer, int submissionId) {
        ProofSubmission submission = submissionDao.findById(submissionId);
        if (submission == null) {
            throw new NotFoundException("Submission #" + submissionId + " was not found.");
        }
        boolean isSubmitter = submission.getSubmittedBy() == viewer.getId();
        boolean canSeeProject = viewer.isAdmin()
                || projectDao.isMember(submission.getProjectId(), viewer.getId());
        if (!isSubmitter && !canSeeProject) {
            throw new com.workproof.exception.AuthorizationException(
                    "You do not have access to this submission.");
        }
        return submission;
    }

    /** Submission history of a task, newest first. Access is checked against the project. */
    public List<ProofSubmission> historyForTask(User viewer, Task task) {
        Project project = projectDao.findById(task.getProjectId());
        String roleInProject = viewer.isAdmin() ? "ADMIN"
                : projectDao.findProjectRole(project.getId(), viewer.getId());
        Permissions.requireViewProject(viewer, project, roleInProject);
        return submissionDao.findByTask(task.getId());
    }

    /** The task a submission belongs to (used by the detail page and the review screen). */
    public Task getTaskForSubmission(ProofSubmission submission) {
        return submission == null ? null : taskDao.findById(submission.getTaskId());
    }

    /** "My submissions" listing for a team member. */
    public PagedResult<ProofSubmission> listMine(User viewer, String status, Integer projectId,
                                                 LocalDate from, LocalDate to, int page, int pageSize) {
        return submissionDao.search(viewer.getId(), projectId, status, from, to, null, page, pageSize);
    }

    /** Submission listing with report style filters, scoped to what the viewer may see. */
    public PagedResult<ProofSubmission> search(User viewer, Integer projectId, Integer submitterId, String status,
                                               LocalDate from, LocalDate to, String search, int page, int pageSize) {
        return submissionDao.search(submitterId, projectId, status, from, to, search, page, pageSize);
    }

    public List<ProofSubmission> findForExport(Integer projectId, String status, LocalDate from, LocalDate to) {
        return submissionDao.findForExport(projectId, status, from, to, 5000);
    }

    public int countForSubmitter(int userId, String status) {
        return submissionDao.countByStatusForSubmitter(userId, status);
    }

    /* ================================ submit ================================ */

    /**
     * Creates a new evidence submission.
     *
     * @param parts the uploaded files, may be empty when the description alone is the proof
     * @return the id of the created submission
     */
    public int submit(User actor, int taskId, String title, String description, List<Part> parts, String ip) {

        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new NotFoundException("Task #" + taskId + " was not found.");
        }
        Project project = projectDao.findById(task.getProjectId());
        String roleInProject = projectDao.findProjectRole(project.getId(), actor.getId());
        List<Integer> assigneeIds = taskDao.findAssigneeIds(taskId);

        Permissions.requireSubmitEvidence(actor, task, project, roleInProject, assigneeIds.contains(actor.getId()));

        // ---- business rules ----
        if (task.getTaskStatus().isAwaitingVerification() && submissionDao.findPendingByTask(taskId) != null) {
            throw new ValidationException("This task already has evidence waiting for review. "
                    + "Please wait for the reviewer's decision before submitting again.");
        }
        if (task.getTaskStatus().isDone()) {
            throw new ValidationException("This task is already completed, so no new evidence is needed.");
        }

        title = ValidationUtil.require(title, "Evidence title", 5, 180);
        description = ValidationUtil.require(description, "Evidence description", 15, 5000);

        List<Part> realParts = new ArrayList<>();
        if (parts != null) {
            for (Part part : parts) {
                if (part != null && part.getSize() > 0
                        && part.getSubmittedFileName() != null
                        && !part.getSubmittedFileName().isBlank()) {
                    realParts.add(part);
                }
            }
        }
        if (realParts.size() > MAX_FILES_PER_SUBMISSION) {
            throw new ValidationException("You can attach at most " + MAX_FILES_PER_SUBMISSION
                    + " files per submission. Please remove " + (realParts.size() - MAX_FILES_PER_SUBMISSION) + ".");
        }
        if (realParts.isEmpty() && description.length() < 40) {
            throw new ValidationException("Please attach at least one file, or write a more detailed "
                    + "description (at least 40 characters) explaining the proof of work.");
        }

        // ---- validate + store the files BEFORE touching the database ----
        Path uploadRoot = AppConfig.uploadDir();
        if (!AppConfig.ensureUploadDirExists()) {
            throw new ValidationException("The evidence storage directory is not writable. "
                    + "Please check the 'workproof.upload.dir' setting.");
        }

        List<FileStorageUtil.StoredFile> storedFiles = new ArrayList<>();
        List<SubmissionFile> fileRows = new ArrayList<>();
        try {
            for (Part part : realParts) {
                FileStorageUtil.StoredFile stored = FileStorageUtil.store(part, uploadRoot);
                storedFiles.add(stored);

                SubmissionFile row = new SubmissionFile();
                row.setOriginalName(stored.getOriginalName());
                row.setStoredName(stored.getStoredName());
                row.setContentType(stored.getContentType() == null ? "application/octet-stream" : stored.getContentType());
                row.setFileSize(stored.getSize());
                row.setSha256(stored.getSha256());
                fileRows.add(row);
            }

            // ---- one transaction: submission + files + task status ----
            ProofSubmission submission = new ProofSubmission();
            submission.setTaskId(taskId);
            submission.setSubmittedBy(actor.getId());
            submission.setTitle(title);
            submission.setDescription(description);
            submission.setVersion(submissionDao.nextVersion(taskId));

            int submissionId = submissionDao.createWithFilesAndTaskStatus(submission, fileRows,
                    TaskStatus.SUBMITTED.name());

            activityService.log(actor, ActivityService.SUBMISSION_CREATED, "SUBMISSION", submissionId,
                    "Submitted evidence '" + title + "' (v" + submission.getVersion() + ", "
                            + fileRows.size() + " file(s)) for task '" + task.getTitle() + "'", ip);

            // ---- tell the manager(s) and reviewer(s) of the project ----
            submission.setId(submissionId);
            notificationService.notifySubmissionReceived(submission, task,
                    projectService.findReviewerIds(task.getProjectId()), actor);

            return submissionId;

        } catch (RuntimeException e) {
            // Roll the files back so we never keep orphaned uploads on disk.
            for (FileStorageUtil.StoredFile stored : storedFiles) {
                FileStorageUtil.delete(uploadRoot, stored.getRelativeLocation());
            }
            throw e;
        }
    }

    /* ============================== file access ============================== */

    /**
     * Authorises a download and returns the file on disk.
     *
     * A user may download evidence when they are:
     *   - the person who submitted it,
     *   - a member of the project it belongs to (managers, reviewers, team members),
     *   - an administrator.
     *
     * The file is never served from a public URL, so this check is the only way in.
     */
    public DownloadResult openForDownload(User viewer, int fileId, String ip) {
        SubmissionFile file = submissionDao.findFile(fileId);
        if (file == null) {
            throw new NotFoundException("File #" + fileId + " was not found.");
        }
        ProofSubmission submission = submissionDao.findById(file.getSubmissionId());
        if (submission == null) {
            throw new NotFoundException("The submission of this file no longer exists.");
        }

        boolean isSubmitter = submission.getSubmittedBy() == viewer.getId();
        boolean isProjectMember = projectDao.isMember(submission.getProjectId(), viewer.getId());
        if (!isSubmitter && !isProjectMember && !viewer.isAdmin()) {
            throw new com.workproof.exception.AuthorizationException(
                    "You are not allowed to download this evidence file.");
        }

        Path uploadRoot = AppConfig.uploadDir();
        Path resolved = FileStorageUtil.resolve(uploadRoot, file.getDerivedRelativeLocation());
        if (resolved == null) {
            throw new NotFoundException("The stored file is missing on the server. "
                    + "It may have been deleted by an administrator.");
        }

        activityService.log(viewer, ActivityService.SUBMISSION_FILE_DOWNLOADED, "SUBMISSION",
                submission.getId(), "Downloaded evidence file '" + file.getOriginalName()
                        + "' (submission v" + submission.getVersion() + ")", ip);

        return new DownloadResult(resolved, file);
    }

    /** Simple value object returned by {@link #openForDownload}. */
    public static class DownloadResult {
        private final Path path;
        private final SubmissionFile file;

        public DownloadResult(Path path, SubmissionFile file) {
            this.path = path;
            this.file = file;
        }

        public Path getPath() { return path; }
        public SubmissionFile getFile() { return file; }
    }

    /* ============================== statistics ============================== */

    public List<SubmissionStatus> allStatuses() {
        return List.of(SubmissionStatus.PENDING, SubmissionStatus.APPROVED,
                SubmissionStatus.REJECTED, SubmissionStatus.SUPERSEDED);
    }
}

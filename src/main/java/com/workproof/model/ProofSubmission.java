package com.workproof.model;

import com.workproof.model.enums.SubmissionStatus;
import com.workproof.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of `proof_submissions` - one attempt to prove a task is done.
 * Old attempts are kept forever, giving the "submission history" required by the spec.
 */
public class ProofSubmission {

    private int id;
    private int taskId;
    private int submittedBy;
    private String title;
    private String description;
    private String status = "PENDING";
    private int version = 1;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    /* ---- joined / calculated ---- */
    private String submitterName;
    private String taskTitle;
    private String taskStatus;
    private int projectId;
    private String projectName;
    private List<SubmissionFile> files = new ArrayList<>();
    private Review review;               // null while pending

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(int submittedBy) { this.submittedBy = submittedBy; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public List<SubmissionFile> getFiles() { return files; }
    public void setFiles(List<SubmissionFile> files) {
        this.files = files == null ? new ArrayList<>() : files;
    }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }

    /* ------------------------------- typed helpers for JSP EL ------------------------------- */

    public SubmissionStatus getSubmissionStatus() {
        return SubmissionStatus.from(status);
    }

    public TaskStatus getParentTaskStatus() {
        return TaskStatus.from(taskStatus);
    }

    public boolean isPending() {
        return getSubmissionStatus() == SubmissionStatus.PENDING;
    }

    public boolean isApproved() {
        return getSubmissionStatus() == SubmissionStatus.APPROVED;
    }

    public boolean isRejected() {
        return getSubmissionStatus() == SubmissionStatus.REJECTED;
    }

    public long getTotalFileSize() {
        long total = 0;
        for (SubmissionFile f : files) {
            total += f.getFileSize();
        }
        return total;
    }

    @Override
    public String toString() {
        return "ProofSubmission{id=" + id + ", taskId=" + taskId + ", version=" + version
                + ", status=" + status + "}";
    }
}

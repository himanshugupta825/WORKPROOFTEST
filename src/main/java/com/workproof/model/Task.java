package com.workproof.model;

import com.workproof.model.enums.TaskPriority;
import com.workproof.model.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Row of the `tasks` table plus assignees and project context. */
public class Task {

    private int id;
    private int projectId;
    private String title;
    private String description;
    private String priority = "MEDIUM";
    private String status = "TODO";
    private LocalDate startDate;
    private LocalDate dueDate;
    private int createdBy;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* ---- joined / calculated ---- */
    private String projectName;
    private String projectCode;
    private String creatorName;
    private List<ProjectMember> assignees = new ArrayList<>();
    private int submissionCount;
    private int pendingReviewCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public List<ProjectMember> getAssignees() { return assignees; }
    public void setAssignees(List<ProjectMember> assignees) {
        this.assignees = assignees == null ? new ArrayList<>() : assignees;
    }

    public int getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(int submissionCount) { this.submissionCount = submissionCount; }

    public int getPendingReviewCount() { return pendingReviewCount; }
    public void setPendingReviewCount(int pendingReviewCount) { this.pendingReviewCount = pendingReviewCount; }

    /* ------------------------------- typed helpers for JSP EL ------------------------------- */

    public TaskStatus getTaskStatus() {
        return TaskStatus.from(status);
    }

    public TaskPriority getTaskPriority() {
        return TaskPriority.from(priority);
    }

    public boolean isOverdue() {
        return dueDate != null && !getTaskStatus().isDone() && dueDate.isBefore(LocalDate.now());
    }

    public boolean isDueSoon() {
        if (dueDate == null || getTaskStatus().isDone()) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        return days >= 0 && days <= 3;
    }

    /** Human friendly "3 days left" / "2 days overdue" text. */
    public String getDeadlineText() {
        if (dueDate == null) {
            return "-";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (getTaskStatus().isDone()) {
            return "Finished";
        }
        if (days == 0) return "Due today";
        if (days == 1) return "Due tomorrow";
        if (days > 1) return days + " days left";
        if (days == -1) return "1 day overdue";
        return Math.abs(days) + " days overdue";
    }

    /** Comma separated assignee names for compact tables. */
    public String getAssigneeNames() {
        if (assignees.isEmpty()) {
            return "Unassigned";
        }
        StringBuilder sb = new StringBuilder();
        for (ProjectMember m : assignees) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(m.getFullName());
        }
        return sb.toString();
    }

    public boolean isUnassigned() {
        return assignees == null || assignees.isEmpty();
    }

    public boolean isAwaitingVerification() {
        return getTaskStatus().isAwaitingVerification();
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title=" + title + ", status=" + status + "}";
    }
}

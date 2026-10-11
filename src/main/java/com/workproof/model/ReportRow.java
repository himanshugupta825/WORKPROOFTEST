package com.workproof.model;

import java.time.LocalDate;

/**
 * A flexible row used by the report pages (project progress, task completion,
 * contribution summary, pending verification, rejected submissions).
 *
 * Instead of five near-identical DTOs, the report service fills only the fields
 * a given report needs - this keeps the project small enough to explain in a viva.
 */
public class ReportRow {

    private int id;
    private String primaryLabel;     // e.g. project name / task title / person name
    private String secondaryLabel;   // e.g. project code / status label
    private String category;         // e.g. priority, role, submission status
    private String owner;            // responsible person
    private LocalDate date;          // due date / submitted date / review date
    private String dateLabel;

    private int totalTasks;
    private int completedTasks;
    private int approvedTasks;
    private int rejectedTasks;
    private int pendingTasks;
    private int overdueTasks;
    private int openTasks;
    private double progressPercent;
    private String note;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPrimaryLabel() { return primaryLabel; }
    public void setPrimaryLabel(String primaryLabel) { this.primaryLabel = primaryLabel; }

    public String getSecondaryLabel() { return secondaryLabel; }
    public void setSecondaryLabel(String secondaryLabel) { this.secondaryLabel = secondaryLabel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getApprovedTasks() { return approvedTasks; }
    public void setApprovedTasks(int approvedTasks) { this.approvedTasks = approvedTasks; }

    public int getRejectedTasks() { return rejectedTasks; }
    public void setRejectedTasks(int rejectedTasks) { this.rejectedTasks = rejectedTasks; }

    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }

    public int getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(int overdueTasks) { this.overdueTasks = overdueTasks; }

    public int getOpenTasks() { return openTasks; }
    public void setOpenTasks(int openTasks) { this.openTasks = openTasks; }

    public double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    /**
     * Bootstrap colour used for the status badge of this row's category column.
     * The category holds a status name (task, project or submission status), so the
     * mapping is kept here instead of in the JSP.
     */
    public String getCategoryBadgeColor() {
        if (category == null) {
            return "secondary";
        }
        switch (category.toUpperCase()) {
            case "APPROVED":
            case "COMPLETED":
                return "success";
            case "REJECTED":
                return "danger";
            case "SUBMITTED":
            case "IN_REVIEW":
            case "PENDING":
                return "warning";
            case "IN_PROGRESS":
            case "ACTIVE":
                return "primary";
            case "ON_HOLD":
                return "warning";
            case "ARCHIVED":
                return "dark";
            default:
                return "secondary";
        }
    }

    public String getProgressColor() {
        if (progressPercent >= 75) return "bg-success";
        if (progressPercent >= 40) return "bg-primary";
        if (progressPercent > 0) return "bg-warning";
        return "bg-secondary";
    }
}

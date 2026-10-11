package com.workproof.model;

import com.workproof.model.enums.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Row of the `projects` table, plus a few calculated fields used by the UI. */
public class Project {

    private int id;
    private String name;
    private String code;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status = "PLANNED";
    private int createdBy;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* ---- joined / calculated fields (filled by the DAO, not stored twice) ---- */
    private String ownerName;
    private int memberCount;
    private int taskCount;
    private int completedTaskCount;
    private int awaitingVerificationCount;
    private int overdueTaskCount;
    /** The logged-in user's role inside this project, e.g. MANAGER - set by the service layer. */
    private String currentUserProjectRole;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }

    public int getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(int completedTaskCount) { this.completedTaskCount = completedTaskCount; }

    public int getAwaitingVerificationCount() { return awaitingVerificationCount; }
    public void setAwaitingVerificationCount(int awaitingVerificationCount) { this.awaitingVerificationCount = awaitingVerificationCount; }

    public int getOverdueTaskCount() { return overdueTaskCount; }
    public void setOverdueTaskCount(int overdueTaskCount) { this.overdueTaskCount = overdueTaskCount; }

    public String getCurrentUserProjectRole() { return currentUserProjectRole; }
    public void setCurrentUserProjectRole(String currentUserProjectRole) { this.currentUserProjectRole = currentUserProjectRole; }

    /* ------------------------------- typed helpers for JSP EL ------------------------------- */

    public ProjectStatus getProjectStatus() {
        return ProjectStatus.from(status);
    }

    /** Progress percentage based on APPROVED + COMPLETED tasks. */
    public int getProgressPercent() {
        if (taskCount <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * completedTaskCount / taskCount);
    }

    public boolean isArchived() {
        return getProjectStatus() == ProjectStatus.ARCHIVED;
    }

    /** Progress bar colour: red when far behind, green when nearly done. */
    public String getProgressColor() {
        int p = getProgressPercent();
        if (p >= 75) return "bg-success";
        if (p >= 40) return "bg-primary";
        if (p > 0) return "bg-warning";
        return "bg-secondary";
    }

    /** "MEMBER" for a plain member, "MANAGER" for managers, null when not a member. */
    public boolean isCurrentUserManager() {
        return "MANAGER".equalsIgnoreCase(currentUserProjectRole);
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", code=" + code + ", name=" + name + "}";
    }
}

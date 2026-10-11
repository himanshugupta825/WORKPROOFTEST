package com.workproof.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Every number shown on a dashboard comes from this object, which the DAO builds
 * from real SQL COUNT queries - no hardcoded metrics anywhere (spec section 10).
 */
public class DashboardStats {

    private int totalProjects;
    private int activeProjects;
    private int totalTasks;
    private int pendingTasks;            // TODO + IN_PROGRESS
    private int completedTasks;          // COMPLETED
    private int awaitingVerification;    // SUBMITTED + IN_REVIEW
    private int approvedSubmissions;
    private int rejectedSubmissions;
    private int pendingSubmissions;
    private int overdueTasks;
    private int totalUsers;
    private int activeUsers;
    private int totalMembers;

    private List<Task> upcomingDeadlines = new ArrayList<>();
    private List<ActivityLog> recentActivity = new ArrayList<>();
    private List<ProofSubmission> awaitingReviewList = new ArrayList<>();

    /* per-status breakdown used by the doughnut chart */
    private int todoTasks;
    private int inProgressTasks;
    private int submittedTasks;
    private int inReviewTasks;
    private int approvedTasks;
    private int rejectedTaskCount;

    public int getTotalProjects() { return totalProjects; }
    public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }

    public int getActiveProjects() { return activeProjects; }
    public void setActiveProjects(int activeProjects) { this.activeProjects = activeProjects; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public int getAwaitingVerification() { return awaitingVerification; }
    public void setAwaitingVerification(int awaitingVerification) { this.awaitingVerification = awaitingVerification; }

    public int getApprovedSubmissions() { return approvedSubmissions; }
    public void setApprovedSubmissions(int approvedSubmissions) { this.approvedSubmissions = approvedSubmissions; }

    public int getRejectedSubmissions() { return rejectedSubmissions; }
    public void setRejectedSubmissions(int rejectedSubmissions) { this.rejectedSubmissions = rejectedSubmissions; }

    public int getPendingSubmissions() { return pendingSubmissions; }
    public void setPendingSubmissions(int pendingSubmissions) { this.pendingSubmissions = pendingSubmissions; }

    public int getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(int overdueTasks) { this.overdueTasks = overdueTasks; }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }

    public List<Task> getUpcomingDeadlines() { return upcomingDeadlines; }
    public void setUpcomingDeadlines(List<Task> upcomingDeadlines) {
        this.upcomingDeadlines = upcomingDeadlines == null ? new ArrayList<>() : upcomingDeadlines;
    }

    public List<ActivityLog> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<ActivityLog> recentActivity) {
        this.recentActivity = recentActivity == null ? new ArrayList<>() : recentActivity;
    }

    public List<ProofSubmission> getAwaitingReviewList() { return awaitingReviewList; }
    public void setAwaitingReviewList(List<ProofSubmission> awaitingReviewList) {
        this.awaitingReviewList = awaitingReviewList == null ? new ArrayList<>() : awaitingReviewList;
    }

    public int getTodoTasks() { return todoTasks; }
    public void setTodoTasks(int todoTasks) { this.todoTasks = todoTasks; }

    public int getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(int inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public int getSubmittedTasks() { return submittedTasks; }
    public void setSubmittedTasks(int submittedTasks) { this.submittedTasks = submittedTasks; }

    public int getInReviewTasks() { return inReviewTasks; }
    public void setInReviewTasks(int inReviewTasks) { this.inReviewTasks = inReviewTasks; }

    public int getApprovedTasks() { return approvedTasks; }
    public void setApprovedTasks(int approvedTasks) { this.approvedTasks = approvedTasks; }

    public int getRejectedTaskCount() { return rejectedTaskCount; }
    public void setRejectedTaskCount(int rejectedTaskCount) { this.rejectedTaskCount = rejectedTaskCount; }

    /**
     * Status breakdown as a small JSON array, ready for the dependency-free
     * doughnut chart in assets/js/workproof.js.
     * The values are integers produced by SQL COUNT queries and the labels and
     * colours are fixed strings from the application, so nothing user supplied can
     * be injected into the markup through this attribute.
     */
    public String getStatusChartJson() {
        return "["
                + segment("To Do", todoTasks, "#94a3b8") + ","
                + segment("In Progress", inProgressTasks, "#2563eb") + ","
                + segment("Submitted", submittedTasks, "#0ea5e9") + ","
                + segment("In Review", inReviewTasks, "#d97706") + ","
                + segment("Approved", approvedTasks, "#16a34a") + ","
                + segment("Rejected", rejectedTaskCount, "#dc2626")
                + "]";
    }

    private String segment(String label, int value, String color) {
        return "{\"label\":\"" + label + "\",\"value\":" + value + ",\"color\":\"" + color + "\"}";
    }

    /** Total of the six chart segments, used for the legend footer. */
    public int getChartTotal() {
        return todoTasks + inProgressTasks + submittedTasks + inReviewTasks + approvedTasks + rejectedTaskCount;
    }

    /** Completion rate for the progress ring on the dashboard. */
    public int getCompletionRate() {
        if (totalTasks <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * completedTasks / totalTasks);
    }

    /** Verification backlog: evidence waiting for a reviewer. */
    public int getVerificationBacklog() {
        return awaitingVerification + pendingSubmissions;
    }
}

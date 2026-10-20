package com.workproof.dto;

import com.workproof.model.AuditLog;
import com.workproof.model.Job;

import java.util.ArrayList;
import java.util.List;

public class JobDashboardStats {
    private long totalJobs;
    private long activeJobs;
    private long completedJobs;
    private long approvedJobs;
    private long totalLocations;
    private long totalCustomers;
    private List<AuditLog> recentActivity = new ArrayList<>();
    private List<Job> recentJobs = new ArrayList<>();

    public JobDashboardStats() {}

    public long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(long totalJobs) { this.totalJobs = totalJobs; }

    public long getActiveJobs() { return activeJobs; }
    public void setActiveJobs(long activeJobs) { this.activeJobs = activeJobs; }

    public long getCompletedJobs() { return completedJobs; }
    public void setCompletedJobs(long completedJobs) { this.completedJobs = completedJobs; }

    public long getApprovedJobs() { return approvedJobs; }
    public void setApprovedJobs(long approvedJobs) { this.approvedJobs = approvedJobs; }

    public long getTotalLocations() { return totalLocations; }
    public void setTotalLocations(long totalLocations) { this.totalLocations = totalLocations; }

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public List<AuditLog> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<AuditLog> recentActivity) { this.recentActivity = recentActivity; }

    public List<Job> getRecentJobs() { return recentJobs; }
    public void setRecentJobs(List<Job> recentJobs) { this.recentJobs = recentJobs; }
}


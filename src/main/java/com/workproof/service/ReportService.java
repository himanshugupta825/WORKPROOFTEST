package com.workproof.service;

import com.workproof.dao.ReportDao;
import com.workproof.dao.UserDao;
import com.workproof.model.ReportRow;
import com.workproof.model.User;
import com.workproof.util.WebUtil;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

/**
 * Reports module (Module 8): every report is built from real database rows and can
 * be exported to CSV.
 *
 * Scoping rule: administrators see the whole system; every other role only sees the
 * projects they take part in. The scope value handed to the DAO is the mechanism that
 * enforces this, so the reports can never leak other teams' data.
 */
public class ReportService {

    /** Available reports, used by the reports page and the export endpoint. */
    public enum ReportType {
        PROJECT_PROGRESS("Project progress report", "project-progress"),
        TASK_COMPLETION("Task completion report", "task-completion"),
        CONTRIBUTION("Individual contribution summary", "contribution"),
        PENDING_VERIFICATION("Pending verification report", "pending-verification"),
        REJECTED_SUBMISSIONS("Rejected submission report", "rejected-submissions");

        private final String label;
        private final String slug;

        ReportType(String label, String slug) {
            this.label = label;
            this.slug = slug;
        }

        public String getLabel() { return label; }
        public String getSlug() { return slug; }

        public static ReportType fromSlug(String slug) {
            if (slug != null) {
                for (ReportType type : values()) {
                    if (type.slug.equalsIgnoreCase(slug.trim())) {
                        return type;
                    }
                }
            }
            return PROJECT_PROGRESS;
        }

        public static ReportType fromName(String name) {
            if (name != null) {
                for (ReportType type : values()) {
                    if (type.name().equalsIgnoreCase(name.trim())) {
                        return type;
                    }
                }
            }
            return PROJECT_PROGRESS;
        }
    }

    private final ReportDao reportDao = new ReportDao();
    private final UserDao userDao = new UserDao();

    /**
     * Runs one report.
     *
     * @param viewer  the logged-in user (drives the data scope)
     * @param type    which report to run
     * @param filters filters coming from the page; unused filters are simply ignored
     */
    public List<ReportRow> run(User viewer, ReportType type, ReportFilters filters) {
        Integer viewerScope = viewer.isAdmin() ? null : viewer.getId();
        LocalDate from = filters.getFrom();
        LocalDate to = filters.getTo();

        switch (type) {
            case TASK_COMPLETION:
                return reportDao.taskCompletion(viewerScope, filters.getProjectId(),
                        filters.getTaskStatus(), from, to);
            case CONTRIBUTION:
                return reportDao.contributionSummary(viewerScope, filters.getProjectId(),
                        filters.getUserId(), from, to);
            case PENDING_VERIFICATION:
                // A member has no review duty; the DAO scope keeps the result empty for them.
                return reportDao.pendingVerification(viewerScope, filters.getProjectId(), from, to);
            case REJECTED_SUBMISSIONS:
                return reportDao.rejectedSubmissions(viewerScope, filters.getProjectId(),
                        filters.getUserId(), from, to);
            case PROJECT_PROGRESS:
            default:
                return reportDao.projectProgress(viewerScope, filters.getProjectStatus(), from, to);
        }
    }

    /** Human readable name of a user filter value, used in the report header. */
    public String describeUser(Integer userId) {
        if (userId == null) {
            return "All users";
        }
        User user = userDao.findById(userId);
        return user == null ? "Unknown user" : user.getFullName();
    }

    /* ================================ CSV ================================ */

    /**
     * Writes a report as CSV.
     *
     * Two safety measures:
     *   - every value is quoted and escaped by {@link WebUtil#csv(String)},
     *   - a value starting with = + - @ gets a leading apostrophe, so spreadsheet
     *     programs do not execute it as a formula (CSV injection).
     */
    public void writeCsv(PrintWriter out, ReportType type, List<ReportRow> rows) {
        out.print(headerFor(type));
        out.print("\n");
        for (ReportRow row : rows) {
            out.print(lineFor(type, row));
            out.print("\n");
        }
        out.flush();
    }

    private String headerFor(ReportType type) {
        switch (type) {
            case TASK_COMPLETION:
                return String.join(",", q("Task"), q("Project"), q("Status"), q("Assignee(s)"),
                        q("Due date"), q("Progress %"), q("Note"));
            case CONTRIBUTION:
                return String.join(",", q("Member"), q("Global role"), q("Department"), q("Assigned tasks"),
                        q("Completed"), q("Approved evidence"), q("Rejected evidence"), q("Pending verification"),
                        q("Overdue"), q("Open"), q("Completion %"));
            case PENDING_VERIFICATION:
                return String.join(",", q("Evidence"), q("Task"), q("Submitted by"), q("Submitted at"),
                        q("Days waiting"), q("Details"));
            case REJECTED_SUBMISSIONS:
                return String.join(",", q("Evidence"), q("Task"), q("Submitter / reviewer"), q("Submitted at"),
                        q("Version"), q("Reviewer feedback"));
            case PROJECT_PROGRESS:
            default:
                return String.join(",", q("Project"), q("Code"), q("Status"), q("Owner"), q("End date"),
                        q("Total tasks"), q("Completed"), q("Approved"), q("Awaiting verification"),
                        q("Rejected"), q("Overdue"), q("Progress %"));
        }
    }

    private String lineFor(ReportType type, ReportRow row) {
        switch (type) {
            case TASK_COMPLETION:
                return String.join(",", q(row.getPrimaryLabel()), q(row.getSecondaryLabel()), q(row.getCategory()),
                        q(row.getOwner()), q(date(row.getDate())), q(trimPercent(row.getProgressPercent())),
                        q(row.getNote()));
            case CONTRIBUTION:
                return String.join(",", q(row.getPrimaryLabel()), q(row.getSecondaryLabel()), q(row.getOwner()),
                        q(row.getTotalTasks()), q(row.getCompletedTasks()), q(row.getApprovedTasks()),
                        q(row.getRejectedTasks()), q(row.getPendingTasks()), q(row.getOverdueTasks()),
                        q(row.getOpenTasks()), q(trimPercent(row.getProgressPercent())));
            case PENDING_VERIFICATION:
                return String.join(",", q(row.getPrimaryLabel()), q(row.getSecondaryLabel()), q(row.getOwner()),
                        q(row.getDate() == null ? "" : row.getDate().toString()), q(row.getOpenTasks()), q(row.getNote()));
            case REJECTED_SUBMISSIONS:
                return String.join(",", q(row.getPrimaryLabel()), q(row.getSecondaryLabel()), q(row.getOwner()),
                        q(row.getDate() == null ? "" : row.getDate().toString()), q(row.getOpenTasks()), q(row.getNote()));
            case PROJECT_PROGRESS:
            default:
                return String.join(",", q(row.getPrimaryLabel()), q(row.getSecondaryLabel()), q(row.getCategory()),
                        q(row.getOwner()), q(date(row.getDate())), q(row.getTotalTasks()), q(row.getCompletedTasks()),
                        q(row.getApprovedTasks()), q(row.getPendingTasks()), q(row.getRejectedTasks()),
                        q(row.getOverdueTasks()), q(trimPercent(row.getProgressPercent())));
        }
    }

    private String date(LocalDate date) {
        return date == null ? "" : WebUtil.DISPLAY_DATE.format(date);
    }

    private String trimPercent(double percent) {
        return percent <= 0 ? "0" : String.format(java.util.Locale.ROOT, "%.1f", percent);
    }

    private String q(Object value) {
        if (value == null) {
            return "\"\"";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return WebUtil.csv(String.valueOf(value));
    }

    /* =========================== filter value object =========================== */

    /** Simple holder for the report filters so the signature stays readable. */
    public static class ReportFilters {
        private Integer projectId;
        private Integer userId;
        private String taskStatus;
        private String projectStatus;
        private LocalDate from;
        private LocalDate to;

        public Integer getProjectId() { return projectId; }
        public void setProjectId(Integer projectId) { this.projectId = projectId; }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getTaskStatus() { return taskStatus; }
        public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }

        public String getProjectStatus() { return projectStatus; }
        public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }

        public LocalDate getFrom() { return from; }
        public void setFrom(LocalDate from) { this.from = from; }

        public LocalDate getTo() { return to; }
        public void setTo(LocalDate to) { this.to = to; }
    }
}

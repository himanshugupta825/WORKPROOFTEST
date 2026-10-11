package com.workproof.controller;

import com.workproof.model.Project;
import com.workproof.model.ReportRow;
import com.workproof.model.User;
import com.workproof.service.ProjectService;
import com.workproof.service.ReportService;
import com.workproof.util.WebUtil;
import com.workproof.service.UserService;
import com.workproof.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Reports page (Module 8). One servlet renders five different reports; the "report"
 * query parameter selects which one, and the same filters work for all of them.
 */
@WebServlet(name = "ReportServlet", urlPatterns = {"/reports"})
public class ReportServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    /** Hard cap so a report on a huge database cannot exhaust the browser. */
    private static final int MAX_ROWS = 2000;

    private final ReportService reportService = new ReportService();
    private final ProjectService projectService = new ProjectService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);

        ReportService.ReportType type = ReportService.ReportType.fromSlug(param(request, "report"));
        ReportService.ReportFilters filters = new ReportService.ReportFilters();
        filters.setProjectId(intParam(request, "projectId"));
        filters.setUserId(intParam(request, "userId"));
        filters.setTaskStatus(ValidationUtil.trimToNull(param(request, "taskStatus")));
        filters.setProjectStatus(ValidationUtil.trimToNull(param(request, "projectStatus")));
        filters.setFrom(WebUtil.dateParamOrNull(request, "from"));
        filters.setTo(WebUtil.dateParamOrNull(request, "to"));

        try {
            List<ReportRow> rows = reportService.run(user, type, filters);
            boolean truncated = rows.size() > MAX_ROWS;
            if (truncated) {
                rows = rows.subList(0, MAX_ROWS);
            }

            List<Project> projects = projectService.findProjectsForUser(user);
            List<User> users = user.isAdmin() ? userService.findAll() : userService.findAssignableUsers();

            request.setAttribute("reportType", type);
            request.setAttribute("reportTypes", ReportService.ReportType.values());
            request.setAttribute("rows", rows);
            request.setAttribute("rowCount", rows.size());
            request.setAttribute("truncated", truncated);
            request.setAttribute("projects", projects);
            request.setAttribute("users", users);
            request.setAttribute("filters", filters);
            request.setAttribute("from", filters.getFrom());
            request.setAttribute("to", filters.getTo());
            request.setAttribute("selectedUserName", reportService.describeUser(filters.getUserId()));

            // Small summary numbers shown above the table.
            request.setAttribute("summary", summarise(rows));
            render(request, response, "reports/index.jsp");

        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }

    /**
     * Aggregates the rows into headline numbers. This is presentation logic over data
     * that is already loaded, so it never triggers extra queries.
     */
    private Summary summarise(List<ReportRow> rows) {
        Summary summary = new Summary();
        summary.rowCount = rows.size();
        double percentSum = 0;
        int percentCount = 0;
        for (ReportRow row : rows) {
            summary.total += row.getTotalTasks();
            summary.completed += row.getCompletedTasks();
            summary.approved += row.getApprovedTasks();
            summary.rejected += row.getRejectedTasks();
            summary.pending += row.getPendingTasks();
            summary.overdue += row.getOverdueTasks();
            if (row.getProgressPercent() > 0) {
                percentSum += row.getProgressPercent();
                percentCount++;
            }
        }
        summary.averageProgress = percentCount == 0 ? 0 : Math.round(percentSum / percentCount);
        return summary;
    }

    /** Simple view-model for the summary strip above every report table. */
    public static class Summary {
        private int rowCount;
        private int total;
        private int completed;
        private int approved;
        private int rejected;
        private int pending;
        private int overdue;
        private long averageProgress;

        public int getRowCount() { return rowCount; }
        public int getTotal() { return total; }
        public int getCompleted() { return completed; }
        public int getApproved() { return approved; }
        public int getRejected() { return rejected; }
        public int getPending() { return pending; }
        public int getOverdue() { return overdue; }
        public long getAverageProgress() { return averageProgress; }
    }
}

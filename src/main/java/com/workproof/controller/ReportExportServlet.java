package com.workproof.controller;

import com.workproof.model.ReportRow;
import com.workproof.model.User;
import com.workproof.service.ReportService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * CSV export of any report. It reuses exactly the same query and the same scoping
 * rules as the on-screen report, so an export can never contain more data than the
 * user is allowed to see.
 */
@WebServlet(name = "ReportExportServlet", urlPatterns = {"/reports/export"})
public class ReportExportServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int MAX_ROWS = 5000;

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);

        try {
            ReportService.ReportType type = ReportService.ReportType.fromSlug(param(request, "report"));
            ReportService.ReportFilters filters = new ReportService.ReportFilters();
            filters.setProjectId(intParam(request, "projectId"));
            filters.setUserId(intParam(request, "userId"));
            filters.setTaskStatus(ValidationUtil.trimToNull(param(request, "taskStatus")));
            filters.setProjectStatus(ValidationUtil.trimToNull(param(request, "projectStatus")));
            filters.setFrom(WebUtil.dateParamOrNull(request, "from"));
            filters.setTo(WebUtil.dateParamOrNull(request, "to"));

            List<ReportRow> rows = reportService.run(user, type, filters);
            if (rows.size() > MAX_ROWS) {
                rows = rows.subList(0, MAX_ROWS);
            }

            String filename = "workproof-" + type.getSlug() + "-" + WebUtil.timestampForFilename() + ".csv";
            try (PrintWriter out = WebUtil.prepareCsvDownload(response, filename)) {
                reportService.writeCsv(out, type, rows);
            }
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

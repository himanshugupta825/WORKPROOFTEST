<%--
  Reports (Module 8).
  One page renders five different reports. Which columns are shown depends on the
  selected report type, and every table offers a CSV export that reuses the same
  query and the same permission scoping.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Reports"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">Reports</h1>
        <p class="wp-breadcrumb mb-0">
            <c:out value="${reportType.label}"/>
            <c:if test="${not currentUser.admin}">&middot; limited to your projects</c:if>
        </p>
    </div>
    <c:if test="${not empty rows}">
        <a class="btn btn-outline-primary"
           href="${pageContext.request.contextPath}/reports/export?report=${reportType.slug}&projectId=${filters.projectId}&userId=${filters.userId}&from=${from}&to=${to}&taskStatus=${filters.taskStatus}&projectStatus=${filters.projectStatus}">
            <i class="bi bi-filetype-csv me-1"></i>Export to CSV
        </a>
    </c:if>
</div>

<!-- ============================= report picker ============================= -->
<div class="wp-card mb-3">
    <div class="wp-card-body pb-2">
        <ul class="nav nav-pills gap-2 mb-3 flex-wrap">
            <c:forEach items="${reportTypes}" var="typeOption">
                <li class="nav-item">
                    <a class="nav-link ${typeOption eq reportType ? 'active' : 'bg-light'}"
                       href="${pageContext.request.contextPath}/reports?report=${typeOption.slug}">
                        <c:out value="${typeOption.label}"/>
                    </a>
                </li>
            </c:forEach>
        </ul>

        <form method="get" action="${pageContext.request.contextPath}/reports" class="row g-2 align-items-end">
            <input type="hidden" name="report" value="${reportType.slug}">

            <div class="col-md-3">
                <label class="form-label" for="projectId">Project</label>
                <select class="form-select" id="projectId" name="projectId">
                    <option value="">All projects</option>
                    <c:forEach items="${projects}" var="project">
                        <option value="${project.id}" ${filters.projectId eq project.id ? 'selected' : ''}>
                            <c:out value="${project.code}"/> &mdash; <c:out value="${wp:shorten(project.name, 26)}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="col-md-3">
                <label class="form-label" for="userId">Team member</label>
                <select class="form-select" id="userId" name="userId">
                    <option value="">All members</option>
                    <c:forEach items="${users}" var="member">
                        <option value="${member.id}" ${filters.userId eq member.id ? 'selected' : ''}>
                            <c:out value="${member.fullName}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>

            <div class="col-md-2">
                <label class="form-label" for="from">From date</label>
                <input type="date" class="form-control" id="from" name="from" value="${from}">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="to">To date</label>
                <input type="date" class="form-control" id="to" name="to" value="${to}">
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary flex-grow-1">
                    <i class="bi bi-funnel me-1"></i>Run
                </button>
                <a class="btn btn-outline-secondary"
                   href="${pageContext.request.contextPath}/reports?report=${reportType.slug}">
                    <i class="bi bi-x-lg"></i>
                </a>
            </div>

            <c:if test="${reportType eq 'TASK_COMPLETION'}">
                <div class="col-md-3">
                    <label class="form-label" for="taskStatus">Task status</label>
                    <select class="form-select" id="taskStatus" name="taskStatus">
                        <option value="ALL">All statuses</option>
                        <option value="TODO" ${filters.taskStatus eq 'TODO' ? 'selected' : ''}>To Do</option>
                        <option value="IN_PROGRESS" ${filters.taskStatus eq 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                        <option value="SUBMITTED" ${filters.taskStatus eq 'SUBMITTED' ? 'selected' : ''}>Submitted</option>
                        <option value="IN_REVIEW" ${filters.taskStatus eq 'IN_REVIEW' ? 'selected' : ''}>In Review</option>
                        <option value="APPROVED" ${filters.taskStatus eq 'APPROVED' ? 'selected' : ''}>Approved</option>
                        <option value="REJECTED" ${filters.taskStatus eq 'REJECTED' ? 'selected' : ''}>Rejected</option>
                        <option value="COMPLETED" ${filters.taskStatus eq 'COMPLETED' ? 'selected' : ''}>Completed</option>
                    </select>
                </div>
            </c:if>

            <c:if test="${reportType eq 'PROJECT_PROGRESS'}">
                <div class="col-md-3">
                    <label class="form-label" for="projectStatus">Project status</label>
                    <select class="form-select" id="projectStatus" name="projectStatus">
                        <option value="ALL">All statuses</option>
                        <option value="PLANNED" ${filters.projectStatus eq 'PLANNED' ? 'selected' : ''}>Planned</option>
                        <option value="ACTIVE" ${filters.projectStatus eq 'ACTIVE' ? 'selected' : ''}>Active</option>
                        <option value="ON_HOLD" ${filters.projectStatus eq 'ON_HOLD' ? 'selected' : ''}>On Hold</option>
                        <option value="COMPLETED" ${filters.projectStatus eq 'COMPLETED' ? 'selected' : ''}>Completed</option>
                        <option value="ARCHIVED" ${filters.projectStatus eq 'ARCHIVED' ? 'selected' : ''}>Archived</option>
                    </select>
                </div>
            </c:if>
        </form>
    </div>
</div>

<!-- ============================== summary ============================== -->
<c:if test="${not empty rows}">
    <div class="row g-3 mb-3">
        <div class="col-6 col-lg-3">
            <div class="wp-card"><div class="wp-card-body py-3">
                <div class="wp-metric-label">Rows</div>
                <div class="fs-4 fw-bold"><c:out value="${summary.rowCount}"/></div>
            </div></div>
        </div>
        <c:if test="${reportType eq 'PROJECT_PROGRESS' or reportType eq 'TASK_COMPLETION'}">
            <div class="col-6 col-lg-3">
                <div class="wp-card"><div class="wp-card-body py-3">
                    <div class="wp-metric-label">Tasks counted</div>
                    <div class="fs-4 fw-bold"><c:out value="${summary.total}"/></div>
                </div></div>
            </div>
        </c:if>
        <div class="col-6 col-lg-3">
            <div class="wp-card"><div class="wp-card-body py-3">
                <div class="wp-metric-label">Approved</div>
                <div class="fs-4 fw-bold text-success"><c:out value="${summary.approved}"/></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="wp-card"><div class="wp-card-body py-3">
                <div class="wp-metric-label">Rejected</div>
                <div class="fs-4 fw-bold text-danger"><c:out value="${summary.rejected}"/></div>
            </div></div>
        </div>
        <div class="col-6 col-lg-3">
            <div class="wp-card"><div class="wp-card-body py-3">
                <div class="wp-metric-label">Overdue</div>
                <div class="fs-4 fw-bold text-warning"><c:out value="${summary.overdue}"/></div>
            </div></div>
        </div>
    </div>
</c:if>

<c:if test="${truncated}">
    <div class="alert alert-warning small">
        <i class="bi bi-exclamation-triangle me-1"></i>
        The result was larger than 2000 rows and has been truncated. Narrow the filters (project or date range)
        to see everything, or export to CSV which allows up to 5000 rows.
    </div>
</c:if>

<!-- =============================== table =============================== -->
<div class="wp-card">
    <div class="wp-card-header">
        <i class="bi bi-table text-primary"></i><strong><c:out value="${reportType.label}"/></strong>
        <span class="ms-auto small text-muted-soft"><c:out value="${rowCount}"/> row(s)</span>
    </div>

    <c:choose>
        <c:when test="${empty rows}">
            <div class="wp-empty">
                <i class="bi bi-bar-chart"></i>
                <h5>No data for this report</h5>
                <p class="mb-0">
                    Either nothing matches the selected filters, or there is no activity yet.
                    Try clearing the filters or choosing another date range.
                </p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table wp-table align-middle mb-0">
                    <thead>
                    <tr>
                        <c:choose>
                            <c:when test="${reportType eq 'TASK_COMPLETION'}">
                                <th>Task</th><th>Project</th><th>Status</th><th>Assignee(s)</th>
                                <th>Due date</th><th>Progress</th><th>Note</th>
                            </c:when>
                            <c:when test="${reportType eq 'CONTRIBUTION'}">
                                <th>Member</th><th>Role</th><th>Department</th><th>Assigned</th>
                                <th>Completed</th><th>Approved</th><th>Rejected</th><th>Pending</th>
                                <th>Overdue</th><th>Completion</th>
                            </c:when>
                            <c:when test="${reportType eq 'PENDING_VERIFICATION'}">
                                <th>Evidence</th><th>Task</th><th>Submitted by</th><th>Submitted</th>
                                <th>Days waiting</th><th>Details</th>
                            </c:when>
                            <c:when test="${reportType eq 'REJECTED_SUBMISSIONS'}">
                                <th>Evidence</th><th>Task</th><th>Submitter / reviewer</th><th>Submitted</th>
                                <th>Version</th><th>Feedback</th>
                            </c:when>
                            <c:otherwise>
                                <th>Project</th><th>Code</th><th>Status</th><th>Owner</th><th>End date</th>
                                <th>Total</th><th>Completed</th><th>Approved</th><th>In review</th>
                                <th>Rejected</th><th>Overdue</th><th>Progress</th>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${rows}" var="row">
                        <tr>
                            <c:choose>
                                <c:when test="${reportType eq 'TASK_COMPLETION'}">
                                    <td class="wp-cell-title"><c:out value="${wp:shorten(row.primaryLabel, 44)}"/></td>
                                    <td class="small"><c:out value="${row.secondaryLabel}"/></td>
                                    <td>
                                        <span class="wp-badge badge-status-${row.categoryBadgeColor}">
                                            <c:out value="${row.category}"/>
                                        </span>
                                    </td>
                                    <td class="small text-muted-soft"><c:out value="${wp:shorten(row.owner, 30)}"/></td>
                                    <td class="small"><c:out value="${wp:date(row.date)}"/></td>
                                    <td style="min-width:110px;">
                                        <div class="wp-progress">
                                            <div class="progress-bar ${row.progressColor}"
                                                 style="width:${row.progressPercent}%"></div>
                                        </div>
                                    </td>
                                    <td class="small text-muted-soft"><c:out value="${row.note}"/></td>
                                </c:when>

                                <c:when test="${reportType eq 'CONTRIBUTION'}">
                                    <td class="wp-cell-title"><c:out value="${row.primaryLabel}"/></td>
                                    <td class="small"><c:out value="${row.secondaryLabel}"/></td>
                                    <td class="small text-muted-soft"><c:out value="${row.owner}"/></td>
                                    <td class="text-center"><c:out value="${row.totalTasks}"/></td>
                                    <td class="text-center text-success"><c:out value="${row.completedTasks}"/></td>
                                    <td class="text-center"><c:out value="${row.approvedTasks}"/></td>
                                    <td class="text-center text-danger"><c:out value="${row.rejectedTasks}"/></td>
                                    <td class="text-center text-warning"><c:out value="${row.pendingTasks}"/></td>
                                    <td class="text-center"><c:out value="${row.overdueTasks}"/></td>
                                    <td style="min-width:120px;">
                                        <div class="wp-progress">
                                            <div class="progress-bar ${row.progressColor}"
                                                 style="width:${row.progressPercent}%"></div>
                                        </div>
                                        <span class="small text-muted-soft">
                                            <c:out value="${wp:percent(row.progressPercent)}"/>
                                        </span>
                                    </td>
                                </c:when>

                                <c:when test="${reportType eq 'PENDING_VERIFICATION'}">
                                    <td class="wp-cell-title"><c:out value="${wp:shorten(row.primaryLabel, 40)}"/></td>
                                    <td class="small"><c:out value="${wp:shorten(row.secondaryLabel, 30)}"/></td>
                                    <td class="small"><c:out value="${row.owner}"/></td>
                                    <td class="small"><c:out value="${wp:dateTime(row.date)}"/></td>
                                    <td class="text-center">
                                        <span class="wp-badge ${row.openTasks gt 2 ? 'badge-status-danger' : 'badge-status-warning'}">
                                            <c:out value="${row.openTasks}"/> day(s)
                                        </span>
                                    </td>
                                    <td class="small text-muted-soft"><c:out value="${row.note}"/></td>
                                </c:when>

                                <c:when test="${reportType eq 'REJECTED_SUBMISSIONS'}">
                                    <td class="wp-cell-title"><c:out value="${wp:shorten(row.primaryLabel, 40)}"/></td>
                                    <td class="small"><c:out value="${wp:shorten(row.secondaryLabel, 30)}"/></td>
                                    <td class="small"><c:out value="${row.owner}"/></td>
                                    <td class="small"><c:out value="${wp:dateTime(row.date)}"/></td>
                                    <td class="text-center">v<c:out value="${row.openTasks}"/></td>
                                    <td class="small text-danger"><c:out value="${wp:shorten(row.note, 90)}"/></td>
                                </c:when>

                                <c:otherwise>
                                    <td class="wp-cell-title"><c:out value="${row.primaryLabel}"/></td>
                                    <td class="small"><c:out value="${row.secondaryLabel}"/></td>
                                    <td class="small"><c:out value="${row.category}"/></td>
                                    <td class="small text-muted-soft"><c:out value="${row.owner}"/></td>
                                    <td class="small"><c:out value="${wp:date(row.date)}"/></td>
                                    <td class="text-center"><c:out value="${row.totalTasks}"/></td>
                                    <td class="text-center text-success"><c:out value="${row.completedTasks}"/></td>
                                    <td class="text-center"><c:out value="${row.approvedTasks}"/></td>
                                    <td class="text-center text-warning"><c:out value="${row.pendingTasks}"/></td>
                                    <td class="text-center text-danger"><c:out value="${row.rejectedTasks}"/></td>
                                    <td class="text-center"><c:out value="${row.overdueTasks}"/></td>
                                    <td style="min-width:130px;">
                                        <div class="wp-progress">
                                            <div class="progress-bar ${row.progressColor}"
                                                 style="width:${row.progressPercent}%"></div>
                                        </div>
                                        <span class="small text-muted-soft">
                                            <c:out value="${wp:percent(row.progressPercent)}"/>
                                        </span>
                                    </td>
                                </c:otherwise>
                            </c:choose>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="alert alert-info small mt-3 mb-0">
    <i class="bi bi-info-circle me-1"></i>
    All figures above are read from the MySQL database with the filters you selected.
    Use <strong>Export to CSV</strong> to hand the report in with your submission; the export respects
    exactly the same permissions as this page.
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

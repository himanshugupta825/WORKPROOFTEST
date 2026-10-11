<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="My evidence"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="mb-3">
    <h1 class="wp-page-title">My evidence submissions</h1>
    <p class="wp-breadcrumb mb-0">
        Every version you submitted, with the reviewer's decision.
        <c:out value="${submissionPage.totalItems}"/> record(s).
    </p>
</div>

<!-- ============================ summary cards ============================ -->
<div class="row g-3 mb-3">
    <div class="col-md-4">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Awaiting review</div>
                    <div class="wp-metric-value"><c:out value="${pendingCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-warning"><i class="bi bi-hourglass-split"></i></div>
            </div>
        </div></div>
    </div>
    <div class="col-md-4">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Approved</div>
                    <div class="wp-metric-value text-success"><c:out value="${approvedCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-success"><i class="bi bi-patch-check"></i></div>
            </div>
        </div></div>
    </div>
    <div class="col-md-4">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Rejected (need rework)</div>
                    <div class="wp-metric-value text-danger"><c:out value="${rejectedCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-danger"><i class="bi bi-x-octagon"></i></div>
            </div>
        </div></div>
    </div>
</div>

<!-- ============================== filters ============================== -->
<div class="wp-card mb-3">
    <div class="wp-card-body py-3">
        <form method="get" action="${pageContext.request.contextPath}/submissions" class="row g-2 align-items-end">
            <div class="col-md-3">
                <label class="form-label" for="status">Status</label>
                <select class="form-select" id="status" name="status">
                    <option value="ALL" ${status eq 'ALL' ? 'selected' : ''}>All statuses</option>
                    <c:forEach items="${statuses}" var="statusOption">
                        <option value="${statusOption}" ${status eq statusOption.name() ? 'selected' : ''}>
                            <c:out value="${statusOption.label}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label" for="projectId">Project</label>
                <select class="form-select" id="projectId" name="projectId">
                    <option value="">All projects</option>
                    <c:forEach items="${projects}" var="project">
                        <option value="${project.id}" ${selectedProjectId eq project.id ? 'selected' : ''}>
                            <c:out value="${project.code}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="from">Submitted from</label>
                <input type="date" class="form-control" id="from" name="from" value="${from}">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="to">Submitted to</label>
                <input type="date" class="form-control" id="to" name="to" value="${to}">
            </div>
            <div class="col-md-2 d-flex gap-1">
                <button type="submit" class="btn btn-primary flex-grow-1"><i class="bi bi-funnel"></i></button>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/submissions">
                    <i class="bi bi-x-lg"></i>
                </a>
            </div>
        </form>
    </div>
</div>

<!-- =============================== table =============================== -->
<div class="wp-card">
    <c:choose>
        <c:when test="${empty submissionPage.items}">
            <div class="wp-empty">
                <i class="bi bi-cloud-arrow-up"></i>
                <h5>No evidence submitted yet</h5>
                <p class="mb-0">
                    Open one of your tasks and use <strong>Submit evidence</strong> to upload
                    screenshots, documents or a written progress note.
                </p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table wp-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th>Evidence</th>
                        <th>Task</th>
                        <th>Project</th>
                        <th>Version</th>
                        <th>Files</th>
                        <th>Submitted</th>
                        <th>Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${submissionPage.items}" var="submission">
                        <tr>
                            <td>
                                <a class="wp-cell-title"
                                   href="${pageContext.request.contextPath}/submissions/view?id=${submission.id}">
                                    <c:out value="${wp:shorten(submission.title, 44)}"/>
                                </a>
                                <c:if test="${submission.rejected}">
                                    <div class="small text-danger">
                                        <i class="bi bi-arrow-repeat me-1"></i>Rework and resubmit
                                    </div>
                                </c:if>
                            </td>
                            <td class="small">
                                <a href="${pageContext.request.contextPath}/tasks/view?id=${submission.taskId}">
                                    <c:out value="${wp:shorten(submission.taskTitle, 32)}"/>
                                </a>
                            </td>
                            <td class="small text-muted-soft"><c:out value="${submission.projectName}"/></td>
                            <td><span class="wp-badge badge-status-secondary">v<c:out value="${submission.version}"/></span></td>
                            <td class="small text-muted-soft">
                                <c:out value="${fn:length(submission.files)}"/> file(s)
                            </td>
                            <td class="small">
                                <c:out value="${wp:dateTime(submission.submittedAt)}"/>
                                <span class="d-block text-muted-soft"><c:out value="${wp:timeAgo(submission.submittedAt)}"/></span>
                            </td>
                            <td>
                                <span class="wp-badge badge-status-${submission.submissionStatus.badgeColor}">
                                    <c:out value="${submission.submissionStatus.label}"/>
                                </span>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${submissionPage.totalPages gt 1}">
    <nav class="mt-4" aria-label="Submission pages">
        <ul class="pagination justify-content-center">
            <li class="page-item ${submissionPage.hasPrevious ? '' : 'disabled'}">
                <a class="page-link" href="?page=${submissionPage.page - 1}&status=${status}&projectId=${selectedProjectId}&from=${from}&to=${to}">Previous</a>
            </li>
            <c:forEach items="${submissionPage.pageNumbers}" var="pageNumber">
                <li class="page-item ${pageNumber eq submissionPage.page ? 'active' : ''}">
                    <a class="page-link" href="?page=${pageNumber}&status=${status}&projectId=${selectedProjectId}&from=${from}&to=${to}">
                        <c:out value="${pageNumber}"/>
                    </a>
                </li>
            </c:forEach>
            <li class="page-item ${submissionPage.hasNext ? '' : 'disabled'}">
                <a class="page-link" href="?page=${submissionPage.page + 1}&status=${status}&projectId=${selectedProjectId}&from=${from}&to=${to}">Next</a>
            </li>
        </ul>
    </nav>
</c:if>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

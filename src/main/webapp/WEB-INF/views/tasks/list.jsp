<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Tasks"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">Tasks</h1>
        <p class="wp-breadcrumb mb-0">
            <c:out value="${taskPage.totalItems}"/> task(s) found
            <c:if test="${scope eq 'mine'}">&middot; filtered to tasks assigned to you</c:if>
        </p>
    </div>
</div>

<!-- ============================== filters ============================== -->
<div class="wp-card mb-3">
    <div class="wp-card-body py-3">
        <form method="get" action="${pageContext.request.contextPath}/tasks" class="row g-2 align-items-end">
            <div class="col-md-3">
                <label class="form-label" for="search">Search</label>
                <input type="search" class="form-control" id="search" name="search"
                       value="<c:out value='${search}'/>" placeholder="Title or description">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="projectId">Project</label>
                <select class="form-select" id="projectId" name="projectId">
                    <option value="">All projects</option>
                    <c:forEach items="${projects}" var="project">
                        <option value="${project.id}"
                            ${selectedProjectId eq project.id ? 'selected' : ''}>
                            <c:out value="${project.code}"/> &mdash; <c:out value="${wp:shorten(project.name, 28)}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="status">Status</label>
                <select class="form-select" id="status" name="status">
                    <option value="ALL" ${status eq 'ALL' ? 'selected' : ''}>All</option>
                    <c:forEach items="${statuses}" var="statusOption">
                        <option value="${statusOption}" ${status eq statusOption.name() ? 'selected' : ''}>
                            <c:out value="${statusOption.label}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="priority">Priority</label>
                <select class="form-select" id="priority" name="priority">
                    <option value="ALL" ${priority eq 'ALL' ? 'selected' : ''}>All</option>
                    <c:forEach items="${priorities}" var="priorityOption">
                        <option value="${priorityOption}" ${priority eq priorityOption.name() ? 'selected' : ''}>
                            <c:out value="${priorityOption.label}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="scope">Scope</label>
                <select class="form-select" id="scope" name="scope">
                    <option value="mine" ${scope eq 'mine' ? 'selected' : ''}>Only my tasks</option>
                    <option value="all" ${scope eq 'all' ? 'selected' : ''}>All visible tasks</option>
                </select>
            </div>
            <div class="col-md-1 d-flex gap-1">
                <button type="submit" class="btn btn-primary flex-grow-1" title="Apply filters">
                    <i class="bi bi-funnel"></i>
                </button>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/tasks"
                   title="Clear"><i class="bi bi-x-lg"></i></a>
            </div>
        </form>
    </div>
</div>

<!-- ================================ table ================================ -->
<div class="wp-card">
    <c:choose>
        <c:when test="${empty taskPage.items}">
            <div class="wp-empty">
                <i class="bi bi-list-check"></i>
                <h5>No tasks found</h5>
                <p class="mb-0">
                    <c:choose>
                        <c:when test="${not empty search or status ne 'ALL' or priority ne 'ALL'}">
                            No task matches the current filters. Try a different search or clear the filters.
                        </c:when>
                        <c:otherwise>
                            Nothing is assigned to you right now.
                            <c:if test="${currentUser.manager or currentUser.admin}">
                                Create tasks from a project page to get started.
                            </c:if>
                        </c:otherwise>
                    </c:choose>
                </p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table wp-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th>
                            <a class="text-decoration-none text-muted-soft"
                               href="?search=${search}&status=${status}&priority=${priority}&scope=${scope}&projectId=${selectedProjectId}&sort=title&dir=${sort eq 'title' and dir eq 'asc' ? 'desc' : 'asc'}">
                                Task <i class="bi bi-arrow-down-up small"></i>
                            </a>
                        </th>
                        <th>Project</th>
                        <th>Assignees</th>
                        <th>
                            <a class="text-decoration-none text-muted-soft"
                               href="?search=${search}&status=${status}&priority=${priority}&scope=${scope}&projectId=${selectedProjectId}&sort=priority&dir=asc">
                                Priority <i class="bi bi-arrow-down-up small"></i>
                            </a>
                        </th>
                        <th>Status</th>
                        <th>
                            <a class="text-decoration-none text-muted-soft"
                               href="?search=${search}&status=${status}&priority=${priority}&scope=${scope}&projectId=${selectedProjectId}&sort=due&dir=${sort eq 'due' and dir eq 'asc' ? 'desc' : 'asc'}">
                                Deadline <i class="bi bi-arrow-down-up small"></i>
                            </a>
                        </th>
                        <th class="text-end">Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${taskPage.items}" var="task">
                        <tr>
                            <td style="min-width:220px;">
                                <a class="wp-cell-title"
                                   href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">
                                    <c:out value="${wp:shorten(task.title, 52)}"/>
                                </a>
                                <c:if test="${task.submissionCount gt 0}">
                                    <span class="wp-badge badge-status-secondary ms-1">
                                        <i class="bi bi-cloud-upload"></i> <c:out value="${task.submissionCount}"/>
                                    </span>
                                </c:if>
                            </td>
                            <td class="small">
                                <a href="${pageContext.request.contextPath}/projects/view?id=${task.projectId}">
                                    <c:out value="${task.projectCode}"/>
                                </a>
                            </td>
                            <td class="small text-muted-soft"><c:out value="${wp:shorten(task.assigneeNames, 28)}"/></td>
                            <td>
                                <span class="wp-badge badge-status-${task.taskPriority.badgeColor}">
                                    <c:out value="${task.taskPriority.label}"/>
                                </span>
                            </td>
                            <td>
                                <span class="wp-badge badge-status-${task.taskStatus.badgeColor}">
                                    <i class="bi ${task.taskStatus.icon}"></i>
                                    <c:out value="${task.taskStatus.label}"/>
                                </span>
                            </td>
                            <td class="small">
                                <c:out value="${wp:date(task.dueDate)}"/>
                                <span class="d-block ${task.overdue ? 'text-danger' : 'text-muted-soft'}">
                                    <c:out value="${task.deadlineText}"/>
                                </span>
                            </td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-outline-primary"
                                   href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">Open</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<!-- ============================== pagination ============================== -->
<c:if test="${taskPage.totalPages gt 1}">
    <nav class="mt-4" aria-label="Task pages">
        <ul class="pagination justify-content-center mb-1">
            <li class="page-item ${taskPage.hasPrevious ? '' : 'disabled'}">
                <a class="page-link"
                   href="?page=${taskPage.page - 1}&search=${search}&status=${status}&priority=${priority}&scope=${scope}&projectId=${selectedProjectId}">Previous</a>
            </li>
            <c:forEach items="${taskPage.pageNumbers}" var="pageNumber">
                <li class="page-item ${pageNumber eq taskPage.page ? 'active' : ''}">
                    <a class="page-link"
                       href="?page=${pageNumber}&search=${search}&status=${status}&priority=${priority}&scope=${scope}&projectId=${selectedProjectId}">
                        <c:out value="${pageNumber}"/>
                    </a>
                </li>
            </c:forEach>
            <li class="page-item ${taskPage.hasNext ? '' : 'disabled'}">
                <a class="page-link"
                   href="?page=${taskPage.page + 1}&search=${search}&status=${status}&priority=${priority}&scope=${scope}&projectId=${selectedProjectId}">Next</a>
            </li>
        </ul>
        <p class="text-center small text-muted-soft">
            Showing <c:out value="${taskPage.firstItemNumber}"/>-<c:out value="${taskPage.lastItemNumber}"/>
            of <c:out value="${taskPage.totalItems}"/>
        </p>
    </nav>
</c:if>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

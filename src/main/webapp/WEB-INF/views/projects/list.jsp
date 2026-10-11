<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Projects"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">Projects</h1>
        <p class="wp-breadcrumb mb-0">
            <c:out value="${projectPage.totalItems}"/> project(s)
            <c:if test="${not currentUser.admin}">&middot; you are a member of these projects</c:if>
        </p>
    </div>
    <c:if test="${currentUser.admin or currentUser.manager}">
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/projects/new">
            <i class="bi bi-plus-lg me-1"></i>Create project
        </a>
    </c:if>
</div>

<!-- ============================== filters ============================== -->
<div class="wp-card mb-3">
    <div class="wp-card-body py-3">
        <form method="get" action="${pageContext.request.contextPath}/projects" class="row g-2 align-items-end">
            <div class="col-md-5">
                <label class="form-label" for="search">Search</label>
                <input type="search" class="form-control" id="search" name="search"
                       value="<c:out value='${search}'/>" placeholder="Project name, code or description">
            </div>
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
            <div class="col-md-2">
                <div class="form-check form-switch">
                    <input class="form-check-input" type="checkbox" role="switch" id="archived" name="archived"
                           value="true" ${archived ? 'checked' : ''}>
                    <label class="form-check-label small" for="archived">Show archived</label>
                </div>
            </div>
            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary flex-grow-1">
                    <i class="bi bi-funnel me-1"></i>Filter
                </button>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/projects"
                   title="Clear filters"><i class="bi bi-x-lg"></i></a>
            </div>
        </form>
    </div>
</div>

<!-- ============================== list ============================== -->
<c:choose>
    <c:when test="${empty projectPage.items}">
        <div class="wp-card">
            <div class="wp-empty">
                <i class="bi bi-kanban"></i>
                <h5>No projects found</h5>
                <p class="mb-3">
                    <c:choose>
                        <c:when test="${not empty search or status ne 'ALL'}">
                            No project matches your filters. Try clearing them.
                        </c:when>
                        <c:when test="${currentUser.admin or currentUser.manager}">
                            You have not created or joined a project yet.
                        </c:when>
                        <c:otherwise>
                            You have not been added to a project yet. Ask your project manager
                            to add you as a member.
                        </c:otherwise>
                    </c:choose>
                </p>
                <c:if test="${currentUser.admin or currentUser.manager}">
                    <a class="btn btn-primary" href="${pageContext.request.contextPath}/projects/new">
                        <i class="bi bi-plus-lg me-1"></i>Create the first project
                    </a>
                </c:if>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row g-3">
            <c:forEach items="${projectPage.items}" var="project">
                <div class="col-md-6 col-xl-4">
                    <div class="wp-card h-100 d-flex flex-column">
                        <div class="wp-card-body d-flex flex-column h-100">
                            <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
                                <span class="wp-badge badge-status-${project.projectStatus.badgeColor}">
                                    <c:out value="${project.projectStatus.label}"/>
                                </span>
                                <span class="small text-muted-soft"><c:out value="${project.code}"/></span>
                            </div>

                            <h5 class="mb-1">
                                <a class="wp-cell-title" href="${pageContext.request.contextPath}/projects/view?id=${project.id}">
                                    <c:out value="${project.name}"/>
                                </a>
                            </h5>
                            <p class="small text-muted-soft flex-grow-1 mb-3">
                                <c:out value="${wp:shorten(project.description, 120)}"/>
                            </p>

                            <div class="mb-2">
                                <div class="d-flex justify-content-between small">
                                    <span>Progress</span>
                                    <span><c:out value="${project.progressPercent}"/>%</span>
                                </div>
                                <div class="wp-progress mt-1">
                                    <div class="progress-bar ${project.progressColor}"
                                         style="width:${project.progressPercent}%"></div>
                                </div>
                            </div>

                            <div class="d-flex flex-wrap gap-2 small text-muted-soft mb-3">
                                <span><i class="bi bi-people me-1"></i><c:out value="${project.memberCount}"/> member(s)</span>
                                <span><i class="bi bi-list-check me-1"></i><c:out value="${project.taskCount}"/> task(s)</span>
                                <c:if test="${project.awaitingVerificationCount gt 0}">
                                    <span class="text-warning">
                                        <i class="bi bi-hourglass me-1"></i><c:out value="${project.awaitingVerificationCount}"/> in review
                                    </span>
                                </c:if>
                                <c:if test="${project.overdueTaskCount gt 0}">
                                    <span class="text-danger">
                                        <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${project.overdueTaskCount}"/> overdue
                                    </span>
                                </c:if>
                            </div>

                            <div class="d-flex justify-content-between align-items-center mt-auto">
                                <span class="small text-muted-soft">
                                    <i class="bi bi-calendar3 me-1"></i>
                                    <c:out value="${wp:date(project.startDate)}"/> &rarr;
                                    <c:out value="${wp:date(project.endDate)}"/>
                                </span>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="${pageContext.request.contextPath}/projects/view?id=${project.id}">
                                    Open
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>

        <!-- ========================== pagination ========================== -->
        <c:if test="${projectPage.totalPages gt 1}">
            <nav class="mt-4" aria-label="Project pages">
                <ul class="pagination justify-content-center mb-1">
                    <li class="page-item ${projectPage.hasPrevious ? '' : 'disabled'}">
                        <a class="page-link"
                           href="?page=${projectPage.page - 1}&search=${search}&status=${status}&archived=${archived}">Previous</a>
                    </li>
                    <c:forEach items="${projectPage.pageNumbers}" var="pageNumber">
                        <li class="page-item ${pageNumber eq projectPage.page ? 'active' : ''}">
                            <a class="page-link"
                               href="?page=${pageNumber}&search=${search}&status=${status}&archived=${archived}">
                                <c:out value="${pageNumber}"/>
                            </a>
                        </li>
                    </c:forEach>
                    <li class="page-item ${projectPage.hasNext ? '' : 'disabled'}">
                        <a class="page-link"
                           href="?page=${projectPage.page + 1}&search=${search}&status=${status}&archived=${archived}">Next</a>
                    </li>
                </ul>
                <p class="text-center small text-muted-soft">
                    Showing <c:out value="${projectPage.firstItemNumber}"/>-<c:out value="${projectPage.lastItemNumber}"/>
                    of <c:out value="${projectPage.totalItems}"/>
                </p>
            </nav>
        </c:if>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

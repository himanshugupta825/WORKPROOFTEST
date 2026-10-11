<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="System activity"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">System activity log</h1>
        <p class="wp-breadcrumb mb-0">
            Immutable audit trail: who performed which action, on what, and when.
            <c:out value="${activityPage.totalItems}"/> entry(ies).
        </p>
    </div>
    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/admin/users">
        <i class="bi bi-people me-1"></i>User management
    </a>
</div>

<div class="wp-card mb-3">
    <div class="wp-card-body py-3">
        <form method="get" action="${pageContext.request.contextPath}/admin/activity" class="row g-2 align-items-end">
            <div class="col-md-3">
                <label class="form-label" for="search">Search</label>
                <input type="search" class="form-control" id="search" name="search"
                       value="<c:out value='${search}'/>" placeholder="Details, action or user">
            </div>
            <div class="col-md-3">
                <label class="form-label" for="action">Action</label>
                <select class="form-select" id="action" name="action">
                    <c:forEach items="${actionTypes}" var="actionOption">
                        <option value="${actionOption}" ${action eq actionOption ? 'selected' : ''}>
                            <c:out value="${actionOption}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="userId">User</label>
                <select class="form-select" id="userId" name="userId">
                    <option value="">All users</option>
                    <c:forEach items="${users}" var="member">
                        <option value="${member.id}" ${selectedUserId eq member.id ? 'selected' : ''}>
                            <c:out value="${wp:shorten(member.fullName, 24)}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label" for="from">From</label>
                <input type="date" class="form-control" id="from" name="from" value="${from}">
            </div>
            <div class="col-md-2">
                <label class="form-label" for="to">To</label>
                <input type="date" class="form-control" id="to" name="to" value="${to}">
            </div>
            <div class="col-12 d-flex gap-2">
                <button class="btn btn-primary"><i class="bi bi-funnel me-1"></i>Apply filters</button>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/admin/activity">
                    Clear
                </a>
            </div>
        </form>
    </div>
</div>

<div class="wp-card">
    <c:choose>
        <c:when test="${empty activityPage.items}">
            <div class="wp-empty">
                <i class="bi bi-shield-check"></i>
                <h5>No log entries</h5>
                <p class="mb-0">No activity matches the selected filters.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table wp-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th>When</th>
                        <th>User</th>
                        <th>Action</th>
                        <th>Entity</th>
                        <th>Details</th>
                        <th>IP</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${activityPage.items}" var="log">
                        <tr>
                            <td class="small">
                                <c:out value="${wp:dateTime(log.createdAt)}"/>
                                <span class="d-block text-muted-soft"><c:out value="${wp:timeAgo(log.createdAt)}"/></span>
                            </td>
                            <td class="small">
                                <c:choose>
                                    <c:when test="${empty log.userName}">
                                        <span class="text-muted-soft">System</span>
                                    </c:when>
                                    <c:otherwise><c:out value="${log.userName}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <span class="wp-badge badge-status-secondary">
                                    <i class="bi ${log.iconClass} me-1"></i><c:out value="${log.action}"/>
                                </span>
                            </td>
                            <td class="small text-muted-soft">
                                <c:if test="${not empty log.entityType}">
                                    <c:out value="${log.entityType}"/>
                                    <c:if test="${not empty log.entityId}">#<c:out value="${log.entityId}"/></c:if>
                                </c:if>
                            </td>
                            <td class="small"><c:out value="${wp:shorten(log.details, 90)}"/></td>
                            <td class="small text-muted-soft"><c:out value="${log.ipAddress}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${activityPage.totalPages gt 1}">
    <nav class="mt-4" aria-label="Activity pages">
        <ul class="pagination justify-content-center">
            <li class="page-item ${activityPage.hasPrevious ? '' : 'disabled'}">
                <a class="page-link"
                   href="?page=${activityPage.page - 1}&search=${search}&action=${action}&userId=${selectedUserId}&from=${from}&to=${to}">Previous</a>
            </li>
            <c:forEach items="${activityPage.pageNumbers}" var="pageNumber">
                <li class="page-item ${pageNumber eq activityPage.page ? 'active' : ''}">
                    <a class="page-link"
                       href="?page=${pageNumber}&search=${search}&action=${action}&userId=${selectedUserId}&from=${from}&to=${to}">
                        <c:out value="${pageNumber}"/>
                    </a>
                </li>
            </c:forEach>
            <li class="page-item ${activityPage.hasNext ? '' : 'disabled'}">
                <a class="page-link"
                   href="?page=${activityPage.page + 1}&search=${search}&action=${action}&userId=${selectedUserId}&from=${from}&to=${to}">Next</a>
            </li>
        </ul>
    </nav>
</c:if>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

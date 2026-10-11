<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Notifications"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">Notifications</h1>
        <p class="wp-breadcrumb mb-0">
            <strong><c:out value="${unreadCount}"/></strong> unread of
            <c:out value="${notificationPage.totalItems}"/> shown
        </p>
    </div>
    <div class="d-flex gap-2">
        <c:if test="${unreadCount gt 0}">
            <form id="wpMarkAllReadForm" method="post"
                  action="${pageContext.request.contextPath}/notifications/action" class="m-0">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="readAll">
                <button type="submit" class="btn btn-outline-primary">
                    <i class="bi bi-check2-all me-1"></i>Mark all as read
                </button>
            </form>
        </c:if>
    </div>
</div>

<!-- ============================== filters ============================== -->
<div class="wp-card mb-3">
    <div class="wp-card-body py-3">
        <form method="get" action="${pageContext.request.contextPath}/notifications" class="row g-2 align-items-end">
            <div class="col-md-4">
                <label class="form-label" for="type">Type</label>
                <select class="form-select" id="type" name="type">
                    <option value="ALL" ${type eq 'ALL' ? 'selected' : ''}>All types</option>
                    <c:forEach items="${types}" var="typeOption">
                        <option value="${typeOption}" ${type eq typeOption.name() ? 'selected' : ''}>
                            <c:out value="${typeOption.name()}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-3">
                <div class="form-check form-switch mt-4">
                    <input class="form-check-input" type="checkbox" role="switch" id="unread" name="unread"
                           value="true" ${unreadOnly ? 'checked' : ''}>
                    <label class="form-check-label" for="unread">Unread only</label>
                </div>
            </div>
            <div class="col-md-3 d-flex gap-2">
                <button class="btn btn-primary flex-grow-1"><i class="bi bi-funnel me-1"></i>Filter</button>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/notifications">
                    <i class="bi bi-x-lg"></i>
                </a>
            </div>
        </form>
    </div>
</div>

<c:choose>
    <c:when test="${empty notificationPage.items}">
        <div class="wp-card">
            <div class="wp-empty">
                <i class="bi bi-bell-slash"></i>
                <h5>No notifications</h5>
                <p class="mb-0">
                    You will be notified when a task is assigned to you, when a deadline is approaching,
                    and when your evidence is approved or rejected.
                </p>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="wp-card">
            <div class="wp-card-body">
                <c:forEach items="${notificationPage.items}" var="note">
                    <div class="wp-notification ${note.read ? '' : 'unread'}">
                        <i class="bi ${note.notificationType.icon} fs-4 text-${note.notificationType.color}"></i>
                        <div class="flex-grow-1 min-width-0">
                            <div class="d-flex flex-wrap align-items-center gap-2">
                                <strong class="small"><c:out value="${note.title}"/></strong>
                                <c:if test="${not note.read}">
                                    <span class="wp-badge badge-status-primary">new</span>
                                </c:if>
                                <span class="small text-muted-soft ms-auto"><c:out value="${note.relativeTime}"/></span>
                            </div>
                            <p class="small text-muted-soft mb-2 mt-1"><c:out value="${note.message}"/></p>
                            <div class="d-flex flex-wrap gap-2">
                                <form method="post" action="${pageContext.request.contextPath}/notifications/action"
                                      class="m-0">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="read">
                                    <input type="hidden" name="id" value="${note.id}">
                                    <input type="hidden" name="link" value="<c:out value='${note.link}'/>">
                                    <button type="submit" class="btn btn-sm btn-outline-primary">
                                        <c:choose>
                                            <c:when test="${empty note.link}">Mark as read</c:when>
                                            <c:otherwise>Open &amp; mark as read</c:otherwise>
                                        </c:choose>
                                    </button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/notifications/action"
                                      class="m-0" data-confirm="Delete this notification?">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${note.id}">
                                    <button type="submit" class="btn btn-sm btn-outline-danger">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <c:if test="${notificationPage.totalPages gt 1}">
            <nav class="mt-4" aria-label="Notification pages">
                <ul class="pagination justify-content-center">
                    <li class="page-item ${notificationPage.hasPrevious ? '' : 'disabled'}">
                        <a class="page-link" href="?page=${notificationPage.page - 1}&type=${type}&unread=${unreadOnly}">Previous</a>
                    </li>
                    <c:forEach items="${notificationPage.pageNumbers}" var="pageNumber">
                        <li class="page-item ${pageNumber eq notificationPage.page ? 'active' : ''}">
                            <a class="page-link" href="?page=${pageNumber}&type=${type}&unread=${unreadOnly}">
                                <c:out value="${pageNumber}"/>
                            </a>
                        </li>
                    </c:forEach>
                    <li class="page-item ${notificationPage.hasNext ? '' : 'disabled'}">
                        <a class="page-link" href="?page=${notificationPage.page + 1}&type=${type}&unread=${unreadOnly}">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

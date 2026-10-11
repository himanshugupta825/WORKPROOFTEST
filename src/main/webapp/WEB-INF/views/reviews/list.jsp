<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Review queue"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">Verification queue</h1>
        <p class="wp-breadcrumb mb-0">
            <strong><c:out value="${pendingCount}"/></strong> evidence submission(s) waiting for your decision
            <c:if test="${not currentUser.admin}">&middot; limited to projects where you are a manager or reviewer</c:if>
        </p>
    </div>
    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/reports?report=pending-verification">
        <i class="bi bi-bar-chart-line me-1"></i>Pending verification report
    </a>
</div>

<c:if test="${selfReviewAllowed}">
    <div class="alert alert-warning small">
        <i class="bi bi-exclamation-triangle me-1"></i>
        Self-review is currently <strong>enabled</strong> by the documented project rule
        <code>workproof.review.allowSelfReview=true</code>. Normally a user cannot review their own submission.
    </div>
</c:if>

<!-- ============================ pending queue ============================ -->
<div class="wp-card mb-4">
    <div class="wp-card-header">
        <i class="bi bi-hourglass-split text-warning"></i><strong>Awaiting verification</strong>
        <span class="ms-auto small text-muted-soft">
            <c:out value="${pendingPage.totalItems}"/> item(s)
        </span>
    </div>

    <div class="wp-card-body pb-0">
        <form method="get" action="${pageContext.request.contextPath}/reviews" class="row g-2 mb-3">
            <div class="col-md-8">
                <input type="search" class="form-control" name="search" value="<c:out value='${search}'/>"
                       placeholder="Search evidence title, task or project">
            </div>
            <div class="col-md-4 d-flex gap-2">
                <button class="btn btn-primary flex-grow-1"><i class="bi bi-search me-1"></i>Search</button>
                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/reviews">
                    <i class="bi bi-x-lg"></i>
                </a>
            </div>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty pendingPage.items}">
            <div class="wp-empty">
                <i class="bi bi-check2-circle"></i>
                <h5>Nothing to review right now</h5>
                <p class="mb-0">When a team member submits evidence for one of your projects it appears here.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table wp-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th>Evidence</th>
                        <th>Task</th>
                        <th>Submitted by</th>
                        <th>Waiting</th>
                        <th>Files</th>
                        <th>Version</th>
                        <th class="text-end">Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${pendingPage.items}" var="submission">
                        <tr>
                            <td>
                                <a class="wp-cell-title"
                                   href="${pageContext.request.contextPath}/reviews/view?id=${submission.id}">
                                    <c:out value="${wp:shorten(submission.title, 40)}"/>
                                </a>
                                <div class="small text-muted-soft"><c:out value="${submission.projectName}"/></div>
                            </td>
                            <td class="small">
                                <a href="${pageContext.request.contextPath}/tasks/view?id=${submission.taskId}">
                                    <c:out value="${wp:shorten(submission.taskTitle, 30)}"/>
                                </a>
                            </td>
                            <td class="small">
                                <span class="wp-avatar wp-avatar-sm me-1">
                                    <c:out value="${wp:initials(submission.submitterName)}"/>
                                </span>
                                <c:out value="${submission.submitterName}"/>
                            </td>
                            <td class="small text-muted-soft"><c:out value="${wp:timeAgo(submission.submittedAt)}"/></td>
                            <td class="small text-muted-soft"><c:out value="${fn:length(submission.files)}"/></td>
                            <td><span class="wp-badge badge-status-secondary">v<c:out value="${submission.version}"/></span></td>
                            <td class="text-end">
                                <a class="btn btn-sm btn-primary"
                                   href="${pageContext.request.contextPath}/reviews/view?id=${submission.id}">
                                    <i class="bi bi-clipboard2-check me-1"></i>Review
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${pendingPage.totalPages gt 1}">
    <nav class="mb-4" aria-label="Queue pages">
        <ul class="pagination justify-content-center">
            <li class="page-item ${pendingPage.hasPrevious ? '' : 'disabled'}">
                <a class="page-link" href="?search=${search}&page=${pendingPage.page - 1}">Previous</a>
            </li>
            <c:forEach items="${pendingPage.pageNumbers}" var="pageNumber">
                <li class="page-item ${pageNumber eq pendingPage.page ? 'active' : ''}">
                    <a class="page-link" href="?search=${search}&page=${pageNumber}"><c:out value="${pageNumber}"/></a>
                </li>
            </c:forEach>
            <li class="page-item ${pendingPage.hasNext ? '' : 'disabled'}">
                <a class="page-link" href="?search=${search}&page=${pendingPage.page + 1}">Next</a>
            </li>
        </ul>
    </nav>
</c:if>

<!-- ============================ review history ============================ -->
<div class="wp-card">
    <div class="wp-card-header">
        <i class="bi bi-clock-history text-primary"></i><strong>My review history</strong>
        <div class="ms-auto">
            <form method="get" action="${pageContext.request.contextPath}/reviews" class="d-flex gap-2">
                <input type="hidden" name="search" value="<c:out value='${search}'/>">
                <select class="form-select form-select-sm" name="decision">
                    <option value="ALL" ${decision eq 'ALL' ? 'selected' : ''}>All decisions</option>
                    <option value="APPROVED" ${decision eq 'APPROVED' ? 'selected' : ''}>Approved only</option>
                    <option value="REJECTED" ${decision eq 'REJECTED' ? 'selected' : ''}>Rejected only</option>
                </select>
                <button type="submit" class="btn btn-sm btn-outline-primary">
                    <i class="bi bi-funnel"></i>
                </button>
            </form>
        </div>
    </div>
    <c:choose>
        <c:when test="${empty historyPage.items}">
            <div class="wp-empty">
                <i class="bi bi-journal-check"></i>
                <p class="mb-0">You have not recorded any decision yet.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-responsive">
                <table class="table wp-table align-middle mb-0">
                    <thead>
                    <tr>
                        <th>Evidence</th>
                        <th>Decision</th>
                        <th>Reviewer</th>
                        <th>Comment</th>
                        <th>Decided</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${historyPage.items}" var="review">
                        <tr>
                            <td>
                                <a class="wp-cell-title"
                                   href="${pageContext.request.contextPath}/submissions/view?id=${review.submissionId}">
                                    <c:out value="${wp:shorten(review.submissionTitle, 40)}"/>
                                </a>
                            </td>
                            <td>
                                <span class="wp-badge badge-status-${review.reviewDecision.badgeColor}">
                                    <c:out value="${review.reviewDecision.label}"/>
                                </span>
                            </td>
                            <td class="small"><c:out value="${review.reviewerName}"/></td>
                            <td class="small text-muted-soft"><c:out value="${wp:shorten(review.comments, 70)}"/></td>
                            <td class="small"><c:out value="${wp:dateTime(review.reviewedAt)}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

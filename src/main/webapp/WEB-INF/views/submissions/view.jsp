<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="${submission.title}"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/submissions">Evidence</a></li>
        <li class="breadcrumb-item">
            <a href="${pageContext.request.contextPath}/tasks/view?id=${submission.taskId}">
                <c:out value="${wp:shorten(submission.taskTitle, 40)}"/>
            </a>
        </li>
        <li class="breadcrumb-item active" aria-current="page">Submission v<c:out value="${submission.version}"/></li>
    </ol>
</nav>

<div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
    <div>
        <h1 class="wp-page-title mb-1"><c:out value="${submission.title}"/></h1>
        <div class="d-flex flex-wrap align-items-center gap-2 small">
            <span class="wp-badge badge-status-${submission.submissionStatus.badgeColor}">
                <c:out value="${submission.submissionStatus.label}"/>
            </span>
            <span class="wp-badge badge-status-secondary">version <c:out value="${submission.version}"/></span>
            <span class="text-muted-soft">
                submitted by <c:out value="${submission.submitterName}"/>
                on <c:out value="${wp:dateTime(submission.submittedAt)}"/>
            </span>
        </div>
    </div>
    <div class="d-flex gap-2">
        <c:if test="${canDecide}">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/reviews/view?id=${submission.id}">
                <i class="bi bi-clipboard2-check me-1"></i>Review this evidence
            </a>
        </c:if>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/tasks/view?id=${submission.taskId}">
            <i class="bi bi-list-check me-1"></i>Open task
        </a>
    </div>
</div>

<div class="row g-3">
    <div class="col-lg-8">
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-card-text text-primary"></i><strong>Description of work</strong></div>
            <div class="wp-card-body">
                <p class="mb-0" style="white-space:pre-line;"><c:out value="${submission.description}"/></p>
            </div>
        </div>

        <div class="wp-card">
            <div class="wp-card-header">
                <i class="bi bi-paperclip text-primary"></i><strong>Attached evidence</strong>
                <span class="ms-auto small text-muted-soft">
                    <c:out value="${fn:length(submission.files)}"/> file(s),
                    <c:out value="${wp:fileSize(submission.totalFileSize)}"/> total
                </span>
            </div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty submission.files}">
                        <div class="wp-empty py-3">
                            <i class="bi bi-file-earmark-x"></i>
                            <p class="mb-0">No files were attached; the written description above is the proof.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach items="${submission.files}" var="file">
                            <div class="wp-file-item">
                                <i class="bi ${file.iconClass} fs-4 text-primary"></i>
                                <div class="flex-grow-1 min-width-0">
                                    <div class="fw-semibold small text-truncate"><c:out value="${file.originalName}"/></div>
                                    <div class="small text-muted-soft">
                                        <c:out value="${wp:fileSize(file.fileSize)}"/> &middot;
                                        <c:out value="${file.contentType}"/> &middot;
                                        sha256 <c:out value="${fn:substring(file.sha256, 0, 12)}"/>...
                                    </div>
                                </div>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="${pageContext.request.contextPath}/submissions/download?id=${file.id}">
                                    <i class="bi bi-download me-1"></i>Download
                                </a>
                            </div>
                        </c:forEach>
                        <p class="small text-muted-soft mb-0 mt-2">
                            <i class="bi bi-info-circle me-1"></i>
                            Downloads are checked on the server and recorded in the activity log.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <div class="col-lg-4">
        <!-- ========================= review outcome ========================= -->
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-clipboard2-check text-primary"></i><strong>Review decision</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty submission.review}">
                        <div class="text-center py-3">
                            <i class="bi bi-hourglass-split fs-2 text-warning"></i>
                            <p class="mb-1 mt-2 fw-semibold">Waiting for a reviewer</p>
                            <p class="small text-muted-soft mb-0">
                                A project manager or reviewer will inspect this evidence and record a decision.
                            </p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <span class="wp-badge badge-status-${submission.review.reviewDecision.badgeColor}">
                                <c:out value="${submission.review.reviewDecision.label}"/>
                            </span>
                            <span class="small text-muted-soft">
                                <c:out value="${wp:dateTime(submission.review.reviewedAt)}"/>
                            </span>
                        </div>
                        <p class="small mb-1">
                            Reviewer: <strong><c:out value="${submission.review.reviewerName}"/></strong>
                        </p>
                        <div class="small p-2 rounded ${submission.review.approved ? 'bg-success-subtle' : 'bg-danger-subtle'}"
                             style="white-space:pre-line;">
                            <c:out value="${submission.review.comments}"/>
                        </div>
                        <c:if test="${submission.rejected}">
                            <a class="btn btn-sm btn-outline-primary w-100 mt-3"
                               href="${pageContext.request.contextPath}/submissions/new?taskId=${submission.taskId}">
                                <i class="bi bi-arrow-repeat me-1"></i>Submit a corrected version
                            </a>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- ========================= version history ========================= -->
        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-clock-history text-primary"></i><strong>Version history</strong></div>
            <div class="wp-card-body">
                <ul class="wp-timeline">
                    <c:forEach items="${history}" var="version">
                        <li>
                            <div class="d-flex justify-content-between gap-2">
                                <a class="small fw-semibold"
                                   href="${pageContext.request.contextPath}/submissions/view?id=${version.id}">
                                    v<c:out value="${version.version}"/>
                                    <c:if test="${version.id eq submission.id}">
                                        <span class="wp-badge badge-status-primary ms-1">current</span>
                                    </c:if>
                                </a>
                                <span class="wp-badge badge-status-${version.submissionStatus.badgeColor}">
                                    <c:out value="${version.submissionStatus.label}"/>
                                </span>
                            </div>
                            <div class="small text-muted-soft">
                                <c:out value="${wp:dateTime(version.submittedAt)}"/> &middot;
                                <c:out value="${fn:length(version.files)}"/> file(s)
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

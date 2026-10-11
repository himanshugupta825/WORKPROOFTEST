<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Submit evidence"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/tasks">Tasks</a></li>
        <li class="breadcrumb-item">
            <a href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">
                <c:out value="${wp:shorten(task.title, 40)}"/>
            </a>
        </li>
        <li class="breadcrumb-item active" aria-current="page">Submit evidence</li>
    </ol>
</nav>

<h1 class="wp-page-title mb-1">Submit proof of work</h1>
<p class="wp-breadcrumb mb-4">
    Task: <strong><c:out value="${task.title}"/></strong> &middot; Project: <c:out value="${task.projectName}"/>
</p>

<div class="row g-3">
    <div class="col-lg-8">
        <div class="wp-card">
            <div class="wp-card-body">
                <form method="post" action="${pageContext.request.contextPath}/submissions/new"
                      enctype="multipart/form-data" novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="taskId" value="${task.id}">

                    <c:if test="${task.taskStatus eq 'SUBMITTED' or task.taskStatus eq 'IN_REVIEW'}">
                        <div class="alert alert-info d-flex align-items-start gap-2" role="alert">
                            <i class="bi bi-info-circle-fill mt-1"></i>
                            <div>
                                This task already has a submission waiting for verification.
                                Uploading again creates a new version, and the earlier waiting version is
                                marked as superseded so your reviewer always sees the newest one.
                            </div>
                        </div>
                    </c:if>

                    <div class="mb-3">
                        <label class="form-label wp-required" for="title">Evidence title</label>
                        <input type="text" class="form-control" id="title" name="title" required maxlength="180"
                               value="<c:out value='${formTitle}'/>"
                               placeholder="e.g. Login and registration screens implemented">
                    </div>

                    <div class="mb-3">
                        <label class="form-label wp-required" for="description">What did you complete?</label>
                        <textarea class="form-control" id="description" name="description" rows="6" required
                                  maxlength="5000"
                                  placeholder="Explain what you built or tested, which files or screens are involved, how it can be verified, and anything the reviewer should know."><c:out value="${formDescription}"/></textarea>
                        <div class="form-text">
                            Be specific: the reviewer uses this description to verify your work.
                            Required even when you attach files.
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label" for="files">Attach evidence files</label>
                        <input class="form-control" type="file" id="files" name="files" multiple
                               accept="<c:out value='${acceptedTypes}'/>" data-file-list="fileList">
                        <div class="form-text">
                            Up to <c:out value="${maxFiles}"/> files, maximum <c:out value="${maxFileSize}"/> each.
                            Allowed: <c:out value="${acceptedTypes}"/>.
                            Files are stored outside the web root and served only to authorised users.
                        </div>
                        <ul class="list-unstyled small mt-2 mb-0" id="fileList"></ul>
                    </div>

                    <div class="alert alert-info small">
                        <i class="bi bi-shield-check me-1"></i>
                        Each file type is verified on the server by both extension and file signature
                        (magic bytes). Please upload real files - renaming a file will be rejected.
                    </div>

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary px-4">
                            <i class="bi bi-cloud-arrow-up me-1"></i>Submit evidence
                        </button>
                        <a class="btn btn-outline-secondary"
                           href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div class="col-lg-4">
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-info-circle text-primary"></i><strong>Task requirements</strong></div>
            <div class="wp-card-body small">
                <p class="mb-2" style="white-space:pre-line;"><c:out value="${wp:shorten(task.description, 400)}"/></p>
                <div class="d-flex justify-content-between">
                    <span class="text-muted-soft">Deadline</span>
                    <span><c:out value="${wp:date(task.dueDate)}"/></span>
                </div>
                <div class="d-flex justify-content-between">
                    <span class="text-muted-soft">Priority</span>
                    <span class="wp-badge badge-status-${task.taskPriority.badgeColor}">
                        <c:out value="${task.taskPriority.label}"/>
                    </span>
                </div>
                <div class="d-flex justify-content-between">
                    <span class="text-muted-soft">Current status</span>
                    <span class="wp-badge badge-status-${task.taskStatus.badgeColor}">
                        <c:out value="${task.taskStatus.label}"/>
                    </span>
                </div>
            </div>
        </div>

        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-clock-history text-primary"></i><strong>Previous versions</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty history}">
                        <p class="small text-muted-soft mb-0">This is the first submission for this task.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="wp-timeline">
                            <c:forEach items="${history}" var="previous">
                                <li>
                                    <div class="d-flex justify-content-between gap-2">
                                        <strong class="small">v<c:out value="${previous.version}"/></strong>
                                        <span class="wp-badge badge-status-${previous.submissionStatus.badgeColor}">
                                            <c:out value="${previous.submissionStatus.label}"/>
                                        </span>
                                    </div>
                                    <div class="small text-muted-soft">
                                        <c:out value="${wp:dateTime(previous.submittedAt)}"/>
                                    </div>
                                    <c:if test="${not empty previous.review and not empty previous.review.comments}">
                                        <div class="small mt-1">
                                            <i class="bi bi-chat-left-text me-1"></i>
                                            <c:out value="${wp:shorten(previous.review.comments, 120)}"/>
                                        </div>
                                    </c:if>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

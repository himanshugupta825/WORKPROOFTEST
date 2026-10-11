<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Review evidence"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/reviews">Review queue</a></li>
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
            <span class="text-muted-soft">
                <c:out value="${submission.projectName}"/> &middot;
                task <a href="${pageContext.request.contextPath}/tasks/view?id=${submission.taskId}">
                    <c:out value="${submission.taskTitle}"/></a>
            </span>
        </div>
    </div>
    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/reviews">
        <i class="bi bi-arrow-left me-1"></i>Back to queue
    </a>
</div>

<div class="row g-3">
    <!-- ========================= submitted evidence ========================= -->
    <div class="col-lg-7">
        <div class="wp-card mb-3">
            <div class="wp-card-header">
                <i class="bi bi-person-check text-primary"></i><strong>Submitted by</strong>
                <span class="ms-auto small text-muted-soft">
                    <c:out value="${wp:dateTime(submission.submittedAt)}"/>
                </span>
            </div>
            <div class="wp-card-body small">
                <div class="d-flex align-items-center gap-2 mb-2">
                    <span class="wp-avatar"><c:out value="${wp:initials(submission.submitterName)}"/></span>
                    <div>
                        <div class="fw-semibold"><c:out value="${submission.submitterName}"/></div>
                        <div class="text-muted-soft">version <c:out value="${submission.version}"/></div>
                    </div>
                </div>
                <p class="mb-0" style="white-space:pre-line;"><c:out value="${submission.description}"/></p>
            </div>
        </div>

        <div class="wp-card mb-3">
            <div class="wp-card-header">
                <i class="bi bi-paperclip text-primary"></i><strong>Evidence files</strong>
                <span class="ms-auto small text-muted-soft"><c:out value="${fn:length(submission.files)}"/> file(s)</span>
            </div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty submission.files}">
                        <p class="small text-muted-soft mb-0">
                            No files attached - the written description is the submitted proof.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach items="${submission.files}" var="file">
                            <div class="wp-file-item">
                                <i class="bi ${file.iconClass} fs-4 text-primary"></i>
                                <div class="flex-grow-1 min-width-0">
                                    <div class="fw-semibold small text-truncate"><c:out value="${file.originalName}"/></div>
                                    <div class="small text-muted-soft">
                                        <c:out value="${wp:fileSize(file.fileSize)}"/> &middot;
                                        <c:out value="${file.contentType}"/>
                                    </div>
                                </div>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="${pageContext.request.contextPath}/submissions/download?id=${file.id}">
                                    <i class="bi bi-download me-1"></i>Open
                                </a>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- ========================= task reference ========================= -->
        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-list-check text-primary"></i><strong>Task requirements</strong></div>
            <div class="wp-card-body small">
                <dl class="row mb-0">
                    <dt class="col-sm-3 text-muted-soft">Task</dt>
                    <dd class="col-sm-9"><c:out value="${task.title}"/></dd>
                    <dt class="col-sm-3 text-muted-soft">Description</dt>
                    <dd class="col-sm-9" style="white-space:pre-line;"><c:out value="${task.description}"/></dd>
                    <dt class="col-sm-3 text-muted-soft">Deadline</dt>
                    <dd class="col-sm-9">
                        <c:out value="${wp:date(task.dueDate)}"/> (<c:out value="${task.deadlineText}"/>)
                    </dd>
                    <dt class="col-sm-3 text-muted-soft">Priority</dt>
                    <dd class="col-sm-9">
                        <span class="wp-badge badge-status-${task.taskPriority.badgeColor}">
                            <c:out value="${task.taskPriority.label}"/>
                        </span>
                    </dd>
                </dl>
            </div>
        </div>
    </div>

    <!-- ============================= decision ============================= -->
    <div class="col-lg-5">
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-clipboard2-check text-primary"></i><strong>Your decision</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${canDecide}">
                        <form method="post" action="${pageContext.request.contextPath}/reviews/decision" novalidate>
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="submissionId" value="${submission.id}">

                            <label class="form-label wp-required" for="comments">Review comments</label>
                            <textarea class="form-control mb-1" id="comments" name="comments" rows="5"
                                      maxlength="3000"
                                      placeholder="Explain what you verified. Feedback is required when rejecting (at least 10 characters)."></textarea>
                            <div class="form-text mb-3">
                                The team member receives these comments as a notification and they are stored
                                permanently with your name and the decision timestamp.
                            </div>

                            <div class="d-grid gap-2">
                                <button type="submit" name="decision" value="APPROVED" class="btn btn-success"
                                        data-confirm="Approve this evidence? The task will be marked as approved.">
                                    <i class="bi bi-patch-check me-1"></i>Approve evidence
                                </button>
                                <button type="submit" name="decision" value="REJECTED" class="btn btn-danger"
                                        data-confirm="Reject this evidence? The member can submit a corrected version afterwards.">
                                    <i class="bi bi-x-octagon me-1"></i>Reject evidence
                                </button>
                            </div>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-secondary small mb-0">
                            <c:choose>
                                <c:when test="${submission.approved or submission.rejected}">
                                    <i class="bi bi-check2-circle me-1"></i>
                                    This submission was already decided:
                                    <strong><c:out value="${submission.submissionStatus.label}"/></strong>.
                                </c:when>
                                <c:when test="${selfReviewBlocked}">
                                    <i class="bi bi-shield-exclamation me-1"></i>
                                    <strong>You submitted this evidence yourself.</strong> WorkProof does not allow
                                    self-approval, so another reviewer or the project manager must decide.
                                </c:when>
                                <c:otherwise>
                                    <i class="bi bi-lock me-1"></i>
                                    You do not have review permission for this project.
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-clock-history text-primary"></i><strong>Version history</strong></div>
            <div class="wp-card-body">
                <ul class="wp-timeline">
                    <c:forEach items="${history}" var="version">
                        <li>
                            <div class="d-flex justify-content-between gap-2">
                                <a class="small fw-semibold"
                                   href="${pageContext.request.contextPath}/reviews/view?id=${version.id}">
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
                            <c:if test="${not empty version.review and not empty version.review.comments}">
                                <div class="small mt-1 text-muted-soft">
                                    <i class="bi bi-chat-left-text me-1"></i>
                                    <c:out value="${wp:shorten(version.review.comments, 90)}"/>
                                </div>
                            </c:if>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

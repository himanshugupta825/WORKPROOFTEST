<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="${task.title}"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/tasks">Tasks</a></li>
        <li class="breadcrumb-item">
            <a href="${pageContext.request.contextPath}/projects/view?id=${project.id}">
                <c:out value="${project.code}"/>
            </a>
        </li>
        <li class="breadcrumb-item active" aria-current="page">Task #<c:out value="${task.id}"/></li>
    </ol>
</nav>

<div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
    <div>
        <h1 class="wp-page-title mb-1"><c:out value="${task.title}"/></h1>
        <div class="d-flex flex-wrap align-items-center gap-2 small">
            <span class="wp-badge badge-status-${task.taskStatus.badgeColor}">
                <i class="bi ${task.taskStatus.icon}"></i> <c:out value="${task.taskStatus.label}"/>
            </span>
            <span class="wp-badge badge-status-${task.taskPriority.badgeColor}">
                Priority: <c:out value="${task.taskPriority.label}"/>
            </span>
            <span class="text-muted-soft">
                <i class="bi bi-calendar3 me-1"></i>Due <c:out value="${wp:date(task.dueDate)}"/>
                (<c:out value="${task.deadlineText}"/>)
            </span>
        </div>
    </div>
    <div class="d-flex flex-wrap gap-2">
        <c:if test="${canSubmit}">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/submissions/new?taskId=${task.id}">
                <i class="bi bi-cloud-arrow-up me-1"></i>Submit evidence
            </a>
        </c:if>
        <c:if test="${canEdit}">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/tasks/edit?id=${task.id}">
                <i class="bi bi-pencil me-1"></i>Edit
            </a>
        </c:if>
        <c:if test="${canEdit and task.submissionCount eq 0}">
            <form method="post" action="${pageContext.request.contextPath}/tasks/action" class="d-inline"
                  data-confirm="Delete this task? This cannot be undone.">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="taskId" value="${task.id}">
                <button type="submit" class="btn btn-outline-danger"><i class="bi bi-trash me-1"></i>Delete</button>
            </form>
        </c:if>
    </div>
</div>

<div class="row g-3">
    <div class="col-lg-8">
        <!-- ============================ description ============================ -->
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-card-text text-primary"></i><strong>Task details</strong></div>
            <div class="wp-card-body">
                <dl class="row mb-0 small">
                    <dt class="col-sm-3 text-muted-soft">Description</dt>
                    <dd class="col-sm-9" style="white-space:pre-line;"><c:out value="${task.description}"/></dd>

                    <dt class="col-sm-3 text-muted-soft">Project</dt>
                    <dd class="col-sm-9">
                        <a href="${pageContext.request.contextPath}/projects/view?id=${project.id}">
                            <c:out value="${project.name}"/>
                        </a>
                    </dd>

                    <dt class="col-sm-3 text-muted-soft">Assigned to</dt>
                    <dd class="col-sm-9">
                        <c:choose>
                            <c:when test="${empty assignees}">
                                <span class="text-muted-soft">Nobody assigned yet</span>
                            </c:when>
                            <c:otherwise>
                                <span class="wp-avatar-stack">
                                    <c:forEach items="${assignees}" var="assignee">
                                        <span class="wp-avatar wp-avatar-sm"
                                              title="<c:out value='${assignee.fullName}'/>">
                                            <c:out value="${wp:initials(assignee.fullName)}"/>
                                        </span>
                                    </c:forEach>
                                </span>
                                <span class="ms-2"><c:out value="${task.assigneeNames}"/></span>
                                <c:if test="${isAssignee}">
                                    <span class="wp-badge badge-status-primary ms-2">you are assigned</span>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </dd>

                    <dt class="col-sm-3 text-muted-soft">Start date</dt>
                    <dd class="col-sm-9"><c:out value="${wp:date(task.startDate)}"/></dd>

                    <dt class="col-sm-3 text-muted-soft">Created by</dt>
                    <dd class="col-sm-9">
                        <c:out value="${task.creatorName}"/> on <c:out value="${wp:dateTime(task.createdAt)}"/>
                    </dd>
                </dl>
            </div>
        </div>

        <!-- ======================= evidence history ======================= -->
        <div class="wp-card">
            <div class="wp-card-header">
                <i class="bi bi-cloud-arrow-up text-primary"></i><strong>Evidence history</strong>
                <span class="ms-auto small text-muted-soft"><c:out value="${fn:length(submissions)}"/> submission(s)</span>
            </div>
            <c:choose>
                <c:when test="${empty submissions}">
                    <div class="wp-empty">
                        <i class="bi bi-inbox"></i>
                        <p class="mb-2">No evidence has been submitted for this task yet.</p>
                        <c:if test="${canSubmit}">
                            <a class="btn btn-sm btn-primary"
                               href="${pageContext.request.contextPath}/submissions/new?taskId=${task.id}">
                                <i class="bi bi-cloud-arrow-up me-1"></i>Submit the first version
                            </a>
                        </c:if>
                    </div>
                </c:when>
                <c:otherwise>
                    <ul class="list-group list-group-flush">
                        <c:forEach items="${submissions}" var="submission">
                            <li class="list-group-item">
                                <div class="d-flex flex-wrap justify-content-between align-items-start gap-2">
                                    <div class="min-width-0">
                                        <a class="wp-cell-title"
                                           href="${pageContext.request.contextPath}/submissions/view?id=${submission.id}">
                                            v<c:out value="${submission.version}"/> &middot;
                                            <c:out value="${submission.title}"/>
                                        </a>
                                        <div class="small text-muted-soft">
                                            by <c:out value="${submission.submitterName}"/> &middot;
                                            <c:out value="${wp:dateTime(submission.submittedAt)}"/> &middot;
                                            <c:out value="${fn:length(submission.files)}"/> file(s)
                                        </div>
                                        <c:if test="${not empty submission.review}">
                                            <div class="small mt-1">
                                                <i class="bi bi-chat-left-text me-1"></i>
                                                <c:out value="${submission.review.reviewerName}"/>:
                                                <span class="${submission.review.approved ? 'text-success' : 'text-danger'}">
                                                    <c:out value="${submission.review.comments}"/>
                                                </span>
                                            </div>
                                        </c:if>
                                    </div>
                                    <span class="wp-badge badge-status-${submission.submissionStatus.badgeColor}">
                                        <c:out value="${submission.submissionStatus.label}"/>
                                    </span>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="col-lg-4">
        <!-- ========================= status changes ========================= -->
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-arrow-repeat text-primary"></i><strong>Change status</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${canChangeStatus}">
                        <form method="post" action="${pageContext.request.contextPath}/tasks/action">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="status">
                            <input type="hidden" name="taskId" value="${task.id}">

                            <label class="form-label" for="status">New status</label>
                            <select class="form-select mb-2" id="status" name="status" required>
                                <c:forEach items="${statuses}" var="statusOption">
                                    <c:if test="${task.taskStatus.canMoveTo(statusOption)}">
                                        <option value="${statusOption}"><c:out value="${statusOption.label}"/></option>
                                    </c:if>
                                </c:forEach>
                            </select>
                            <div class="form-text mb-2">
                                Only valid transitions are listed. Approved and Rejected are set by
                                the reviewer through the evidence review screen.
                            </div>

                            <label class="form-label" for="comment">Note (optional)</label>
                            <input type="text" class="form-control mb-3" id="comment" name="comment" maxlength="200"
                                   placeholder="e.g. Waiting for the database backup">

                            <button type="submit" class="btn btn-primary w-100">
                                <i class="bi bi-check2 me-1"></i>Update status
                            </button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <p class="small text-muted-soft mb-0">
                            You cannot change the status of this task.
                            <c:choose>
                                <c:when test="${currentUser.reviewer}">
                                    Reviewers decide through the evidence review screen.
                                </c:when>
                                <c:otherwise>
                                    Only the assignee or the project manager can move this task.
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- ============================ timeline ============================ -->
        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-clock-history text-primary"></i><strong>Task timeline</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty activity}">
                        <p class="small text-muted-soft mb-0">No recorded changes yet.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="wp-timeline">
                            <c:forEach items="${activity}" var="log">
                                <li>
                                    <div class="d-flex gap-2">
                                        <i class="bi ${log.iconClass} text-primary"></i>
                                        <strong class="small"><c:out value="${log.actionLabel}"/></strong>
                                        <span class="wp-timeline-time ms-auto">
                                            <c:out value="${wp:timeAgo(log.createdAt)}"/>
                                        </span>
                                    </div>
                                    <div class="small text-muted-soft">
                                        <c:out value="${log.details}"/> &middot; <c:out value="${log.actorName}"/>
                                    </div>
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

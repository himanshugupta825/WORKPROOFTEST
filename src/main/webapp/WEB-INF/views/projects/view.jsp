<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="${project.name}"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/projects">Projects</a></li>
        <li class="breadcrumb-item active" aria-current="page"><c:out value="${project.code}"/></li>
    </ol>
</nav>

<div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
    <div>
        <h1 class="wp-page-title mb-1"><c:out value="${project.name}"/></h1>
        <div class="d-flex flex-wrap align-items-center gap-2 small">
            <span class="wp-badge badge-status-${project.projectStatus.badgeColor}">
                <c:out value="${project.projectStatus.label}"/>
            </span>
            <span class="text-muted-soft">
                <i class="bi bi-calendar3 me-1"></i>
                <c:out value="${wp:date(project.startDate)}"/> &rarr; <c:out value="${wp:date(project.endDate)}"/>
            </span>
            <span class="text-muted-soft">
                <i class="bi bi-person-badge me-1"></i>Owner: <c:out value="${project.ownerName}"/>
            </span>
            <c:if test="${not empty project.currentUserProjectRole and project.currentUserProjectRole ne 'ADMIN'}">
                <span class="wp-badge badge-status-primary">
                    Your role: <c:out value="${project.currentUserProjectRole}"/>
                </span>
            </c:if>
        </div>
    </div>
    <div class="d-flex flex-wrap gap-2">
        <c:if test="${canManage}">
            <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/tasks/new?projectId=${project.id}">
                <i class="bi bi-plus-lg me-1"></i>New task
            </a>
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/projects/edit?id=${project.id}">
                <i class="bi bi-pencil me-1"></i>Edit
            </a>
        </c:if>
        <c:if test="${canReview}">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/reviews">
                <i class="bi bi-clipboard2-check me-1"></i>Review queue
            </a>
        </c:if>
        <c:if test="${canManage}">
            <c:choose>
                <c:when test="${project.archived}">
                    <form method="post" action="${pageContext.request.contextPath}/projects/action" class="d-inline">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="restore">
                        <input type="hidden" name="projectId" value="${project.id}">
                        <button type="submit" class="btn btn-outline-success">
                            <i class="bi bi-arrow-counterclockwise me-1"></i>Restore
                        </button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${pageContext.request.contextPath}/projects/action" class="d-inline"
                          data-confirm="Archive this project? It will be hidden from listings but all history is kept.">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="archive">
                        <input type="hidden" name="projectId" value="${project.id}">
                        <button type="submit" class="btn btn-outline-danger">
                            <i class="bi bi-archive me-1"></i>Archive
                        </button>
                    </form>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-lg-8">
        <div class="wp-card h-100">
            <div class="wp-card-header"><i class="bi bi-info-circle text-primary"></i><strong>Description</strong></div>
            <div class="wp-card-body">
                <p class="mb-0" style="white-space:pre-line;"><c:out value="${project.description}"/></p>
            </div>
        </div>
    </div>
    <div class="col-lg-4">
        <div class="wp-card h-100">
            <div class="wp-card-header"><i class="bi bi-graph-up-arrow text-primary"></i><strong>Progress</strong></div>
            <div class="wp-card-body">
                <div class="d-flex justify-content-between align-items-end mb-2">
                    <span class="display-6 fw-bold"><c:out value="${project.progressPercent}"/>%</span>
                    <span class="small text-muted-soft">
                        <c:out value="${project.completedTaskCount}"/> / <c:out value="${project.taskCount}"/> tasks done
                    </span>
                </div>
                <div class="wp-progress mb-3">
                    <div class="progress-bar ${project.progressColor}" style="width:${project.progressPercent}%"></div>
                </div>
                <div class="row text-center g-2 small">
                    <div class="col-6 col-lg-3">
                        <div class="fw-bold"><c:out value="${project.memberCount}"/></div>
                        <div class="text-muted-soft">Members</div>
                    </div>
                    <div class="col-6 col-lg-3">
                        <div class="fw-bold"><c:out value="${project.taskCount}"/></div>
                        <div class="text-muted-soft">Tasks</div>
                    </div>
                    <div class="col-6 col-lg-3">
                        <div class="fw-bold text-warning"><c:out value="${project.awaitingVerificationCount}"/></div>
                        <div class="text-muted-soft">In review</div>
                    </div>
                    <div class="col-6 col-lg-3">
                        <div class="fw-bold text-danger"><c:out value="${project.overdueTaskCount}"/></div>
                        <div class="text-muted-soft">Overdue</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-3">
    <!-- ============================== tasks ============================== -->
    <div class="col-xl-7">
        <div class="wp-card h-100">
            <div class="wp-card-header">
                <i class="bi bi-list-check text-primary"></i><strong>Tasks</strong>
                <span class="ms-auto small text-muted-soft"><c:out value="${fn:length(tasks)}"/> total</span>
            </div>
            <c:choose>
                <c:when test="${empty tasks}">
                    <div class="wp-empty">
                        <i class="bi bi-clipboard-plus"></i>
                        <p class="mb-2">No tasks in this project yet.</p>
                        <c:if test="${canManage}">
                            <a class="btn btn-sm btn-primary"
                               href="${pageContext.request.contextPath}/tasks/new?projectId=${project.id}">
                                <i class="bi bi-plus-lg me-1"></i>Create the first task
                            </a>
                        </c:if>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="table wp-table align-middle">
                            <thead>
                            <tr>
                                <th>Task</th>
                                <th>Assignees</th>
                                <th>Priority</th>
                                <th>Status</th>
                                <th>Deadline</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${tasks}" var="task">
                                <tr>
                                    <td>
                                        <a class="wp-cell-title"
                                           href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">
                                            <c:out value="${wp:shorten(task.title, 48)}"/>
                                        </a>
                                        <c:if test="${task.pendingReviewCount gt 0}">
                                            <span class="wp-badge badge-status-warning ms-1">evidence pending</span>
                                        </c:if>
                                    </td>
                                    <td class="small text-muted-soft"><c:out value="${task.assigneeNames}"/></td>
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
                                        <c:if test="${task.overdue}">
                                            <span class="text-danger d-block">overdue</span>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="col-xl-5">
        <!-- ============================= members ============================= -->
        <div class="wp-card mb-3">
            <div class="wp-card-header">
                <i class="bi bi-people text-primary"></i><strong>Project members</strong>
                <span class="ms-auto small text-muted-soft"><c:out value="${fn:length(members)}"/></span>
            </div>
            <ul class="list-group list-group-flush">
                <c:forEach items="${members}" var="member">
                    <li class="list-group-item d-flex align-items-center gap-2">
                        <span class="wp-avatar wp-avatar-sm"><c:out value="${wp:initials(member.fullName)}"/></span>
                        <div class="min-width-0 flex-grow-1">
                            <div class="fw-semibold small text-truncate"><c:out value="${member.fullName}"/></div>
                            <div class="small text-muted-soft text-truncate"><c:out value="${member.email}"/></div>
                        </div>
                        <span class="wp-badge badge-status-secondary"><c:out value="${member.projectRole.label}"/></span>
                        <c:if test="${canManage}">
                            <form method="post" action="${pageContext.request.contextPath}/projects/action"
                                  class="m-0" data-confirm="Remove this member? Their task assignments in this project will be released.">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="removeMember">
                                <input type="hidden" name="projectId" value="${project.id}">
                                <input type="hidden" name="userId" value="${member.userId}">
                                <button type="submit" class="btn btn-sm btn-outline-danger" title="Remove member">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </form>
                        </c:if>
                    </li>
                </c:forEach>
            </ul>

            <c:if test="${canManage}">
                <div class="wp-card-body border-top">
                    <h6 class="mb-2">Add a member</h6>
                    <c:choose>
                        <c:when test="${empty candidates}">
                            <p class="small text-muted-soft mb-0">
                                Every active user is already a member of this project.
                            </p>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/projects/action"
                                  class="row g-2">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="addMember">
                                <input type="hidden" name="projectId" value="${project.id}">
                                <div class="col-12">
                                    <label class="form-label small" for="userId">User</label>
                                    <select class="form-select form-select-sm" id="userId" name="userId" required>
                                        <option value="">Choose a user...</option>
                                        <c:forEach items="${candidates}" var="candidate">
                                            <option value="${candidate.id}">
                                                <c:out value="${candidate.fullName}"/> &mdash;
                                                <c:out value="${candidate.email}"/> (<c:out value="${candidate.role.label}"/>)
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-8">
                                    <label class="form-label small" for="roleInProject">Project role</label>
                                    <select class="form-select form-select-sm" id="roleInProject" name="roleInProject">
                                        <c:forEach items="${projectRoles}" var="roleOption">
                                            <option value="${roleOption}"><c:out value="${roleOption.label}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="col-4 d-flex align-items-end">
                                    <button type="submit" class="btn btn-sm btn-primary w-100">
                                        <i class="bi bi-person-plus"></i> Add
                                    </button>
                                </div>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>
        </div>

        <!-- ============================= activity ============================= -->
        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-activity text-primary"></i><strong>Recent activity</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty activity}">
                        <div class="wp-empty py-3">
                            <i class="bi bi-activity"></i>
                            <p class="mb-0 small">No activity recorded for this project yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ul class="wp-timeline">
                            <c:forEach items="${activity}" var="log">
                                <li>
                                    <div class="d-flex gap-2">
                                        <i class="bi ${log.iconClass} text-primary"></i>
                                        <strong class="small"><c:out value="${log.actionLabel}"/></strong>
                                        <span class="wp-timeline-time ms-auto"><c:out value="${wp:timeAgo(log.createdAt)}"/></span>
                                    </div>
                                    <div class="small text-muted-soft"><c:out value="${log.details}"/></div>
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

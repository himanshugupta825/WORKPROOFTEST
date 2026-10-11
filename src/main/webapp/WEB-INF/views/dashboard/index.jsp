<%--
  Role dashboard (Module 2).
  Every number on this page comes from DashboardStats, which is filled by real
  COUNT/SUM queries scoped to the signed-in user's role - nothing is hardcoded.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Dashboard"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-4">
    <div>
        <h1 class="wp-page-title">Hello, <c:out value="${currentUser.fullName}"/></h1>
        <p class="wp-breadcrumb mb-0">
            <span class="wp-badge badge-status-primary"><c:out value="${currentUser.role.label}"/></span>
            <span class="ms-2">
                <c:choose>
                    <c:when test="${currentUser.admin}">System-wide view of every project, task and submission.</c:when>
                    <c:when test="${currentUser.manager}">Progress of the projects you manage.</c:when>
                    <c:when test="${currentUser.reviewer}">Verification workload of the projects assigned to you.</c:when>
                    <c:otherwise>Your assigned tasks and evidence status.</c:otherwise>
                </c:choose>
            </span>
        </p>
    </div>
    <div class="d-flex gap-2">
        <c:if test="${currentUser.admin or currentUser.manager}">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/projects/new">
                <i class="bi bi-plus-lg me-1"></i>New project
            </a>
        </c:if>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/tasks?scope=mine">
            <i class="bi bi-list-check me-1"></i>My tasks
        </a>
    </div>
</div>

<!-- ============================ metric cards ============================ -->
<div class="row g-3 mb-4">
    <div class="col-sm-6 col-xl-3">
        <div class="wp-card h-100"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Projects</div>
                    <div class="wp-metric-value"><c:out value="${stats.totalProjects}"/></div>
                    <div class="small text-muted-soft">
                        <c:out value="${stats.activeProjects}"/> active
                    </div>
                </div>
                <div class="wp-metric-icon wp-icon-primary"><i class="bi bi-kanban"></i></div>
            </div>
        </div></div>
    </div>

    <div class="col-sm-6 col-xl-3">
        <div class="wp-card h-100"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Total tasks</div>
                    <div class="wp-metric-value"><c:out value="${stats.totalTasks}"/></div>
                    <div class="small text-muted-soft">
                        <c:out value="${stats.pendingTasks}"/> open &middot;
                        <c:out value="${stats.completedTasks}"/> completed
                    </div>
                </div>
                <div class="wp-metric-icon wp-icon-info"><i class="bi bi-list-check"></i></div>
            </div>
        </div></div>
    </div>

    <div class="col-sm-6 col-xl-3">
        <div class="wp-card h-100"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Awaiting verification</div>
                    <div class="wp-metric-value"><c:out value="${stats.awaitingVerification}"/></div>
                    <div class="small text-muted-soft">
                        <c:out value="${stats.pendingSubmissions}"/> submission(s) pending
                    </div>
                </div>
                <div class="wp-metric-icon wp-icon-warning"><i class="bi bi-hourglass-split"></i></div>
            </div>
        </div></div>
    </div>

    <div class="col-sm-6 col-xl-3">
        <div class="wp-card h-100"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Verification results</div>
                    <div class="wp-metric-value">
                        <span class="text-success"><c:out value="${stats.approvedSubmissions}"/></span>
                        <span class="fs-6 text-muted-soft">/</span>
                        <span class="text-danger"><c:out value="${stats.rejectedSubmissions}"/></span>
                    </div>
                    <div class="small text-muted-soft">approved / rejected submissions</div>
                </div>
                <div class="wp-metric-icon wp-icon-success"><i class="bi bi-patch-check"></i></div>
            </div>
        </div></div>
    </div>
</div>

<c:if test="${stats.overdueTasks gt 0}">
    <div class="alert alert-warning d-flex align-items-center gap-2" role="alert">
        <i class="bi bi-exclamation-triangle-fill"></i>
        <div>
            <strong><c:out value="${stats.overdueTasks}"/> task(s) are past their deadline.</strong>
            <a class="alert-link ms-1" href="${pageContext.request.contextPath}/tasks?scope=mine">Review them now</a>
        </div>
    </div>
</c:if>

<div class="row g-3 mb-4">
    <!-- ======================= chart + completion ======================= -->
    <div class="col-lg-7">
        <div class="wp-card h-100">
            <div class="wp-card-header">
                <i class="bi bi-pie-chart text-primary"></i>
                <strong>Task status distribution</strong>
                <span class="ms-auto small text-muted-soft"><c:out value="${stats.chartTotal}"/> task(s) in scope</span>
            </div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${stats.chartTotal eq 0}">
                        <div class="wp-empty">
                            <i class="bi bi-clipboard-data"></i>
                            <p class="mb-1">No tasks yet.</p>
                            <p class="small mb-0">
                                <c:choose>
                                    <c:when test="${currentUser.admin or currentUser.manager}">
                                        Create a project and add tasks to see progress here.
                                    </c:when>
                                    <c:otherwise>As soon as you are assigned a task it will appear here.</c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="row align-items-center g-3">
                            <div class="col-sm-5">
                                <div class="wp-doughnut" data-chart='${stats.statusChartJson}'></div>
                            </div>
                            <div class="col-sm-7">
                                <ul class="wp-legend">
                                    <li><span class="wp-dot" style="background:#94a3b8"></span>To Do
                                        <strong class="ms-auto"><c:out value="${stats.todoTasks}"/></strong></li>
                                    <li><span class="wp-dot" style="background:#2563eb"></span>In Progress
                                        <strong class="ms-auto"><c:out value="${stats.inProgressTasks}"/></strong></li>
                                    <li><span class="wp-dot" style="background:#0ea5e9"></span>Submitted
                                        <strong class="ms-auto"><c:out value="${stats.submittedTasks}"/></strong></li>
                                    <li><span class="wp-dot" style="background:#d97706"></span>In Review
                                        <strong class="ms-auto"><c:out value="${stats.inReviewTasks}"/></strong></li>
                                    <li><span class="wp-dot" style="background:#16a34a"></span>Approved
                                        <strong class="ms-auto"><c:out value="${stats.approvedTasks}"/></strong></li>
                                    <li><span class="wp-dot" style="background:#dc2626"></span>Rejected
                                        <strong class="ms-auto"><c:out value="${stats.rejectedTaskCount}"/></strong></li>
                                </ul>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- ============================ completion ============================ -->
    <div class="col-lg-5">
        <div class="wp-card h-100">
            <div class="wp-card-header">
                <i class="bi bi-speedometer text-primary"></i><strong>Completion</strong>
            </div>
            <div class="wp-card-body text-center">
                <div class="wp-ring mx-auto mb-3" data-ring="${stats.completionRate}">
                    <span><c:out value="${stats.completionRate}"/>%</span>
                </div>
                <p class="text-muted-soft mb-3">
                    <c:out value="${stats.completedTasks}"/> of <c:out value="${stats.totalTasks}"/>
                    task(s) fully completed.
                </p>
                <div class="row text-center g-2">
                    <div class="col-4">
                        <div class="fw-bold"><c:out value="${stats.pendingTasks}"/></div>
                        <div class="small text-muted-soft">Open</div>
                    </div>
                    <div class="col-4">
                        <div class="fw-bold"><c:out value="${stats.awaitingVerification}"/></div>
                        <div class="small text-muted-soft">In review</div>
                    </div>
                    <div class="col-4">
                        <div class="fw-bold text-danger"><c:out value="${stats.overdueTasks}"/></div>
                        <div class="small text-muted-soft">Overdue</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-3">
    <!-- ========================= upcoming deadlines ========================= -->
    <div class="col-lg-4">
        <div class="wp-card h-100">
            <div class="wp-card-header">
                <i class="bi bi-alarm text-primary"></i><strong>Upcoming deadlines</strong>
                <a class="ms-auto small" href="${pageContext.request.contextPath}/tasks?scope=mine">All tasks</a>
            </div>
            <div class="wp-card-body p-0">
                <c:choose>
                    <c:when test="${empty upcomingTasks}">
                        <div class="wp-empty">
                            <i class="bi bi-calendar-check"></i>
                            <p class="mb-0">No deadlines in the near future.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-group list-group-flush">
                            <c:forEach items="${upcomingTasks}" var="task">
                                <li class="list-group-item d-flex justify-content-between align-items-start gap-2">
                                    <div class="min-width-0">
                                        <a class="wp-cell-title d-block text-truncate"
                                           href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">
                                            <c:out value="${task.title}"/>
                                        </a>
                                        <span class="small text-muted-soft">
                                            <c:out value="${task.projectName}"/> &middot;
                                            <c:out value="${wp:date(task.dueDate)}"/>
                                        </span>
                                    </div>
                                    <span class="wp-badge ${task.overdue ? 'badge-status-danger' : (task.dueSoon ? 'badge-status-warning' : 'badge-status-secondary')}">
                                        <c:out value="${task.deadlineText}"/>
                                    </span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- ============================ review queue ============================ -->
    <div class="col-lg-4">
        <div class="wp-card h-100">
            <div class="wp-card-header">
                <i class="bi bi-clipboard2-check text-primary"></i><strong>Waiting for verification</strong>
                <c:if test="${currentUser.admin or currentUser.manager or currentUser.reviewer}">
                    <a class="ms-auto small" href="${pageContext.request.contextPath}/reviews">Review queue</a>
                </c:if>
            </div>
            <div class="wp-card-body p-0">
                <c:choose>
                    <c:when test="${empty stats.awaitingReviewList}">
                        <div class="wp-empty">
                            <i class="bi bi-check2-circle"></i>
                            <p class="mb-0">Nothing is waiting for a decision.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-group list-group-flush">
                            <c:forEach items="${stats.awaitingReviewList}" var="submission">
                                <li class="list-group-item">
                                    <a class="wp-cell-title d-block text-truncate"
                                       href="${pageContext.request.contextPath}/submissions/view?id=${submission.id}">
                                        <c:out value="${submission.title}"/>
                                    </a>
                                    <span class="small text-muted-soft">
                                        <c:out value="${submission.taskTitle}"/> &middot;
                                        <c:out value="${submission.submitterName}"/> &middot;
                                        <c:out value="${wp:timeAgo(submission.submittedAt)}"/>
                                    </span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- ============================ notifications ============================ -->
    <div class="col-lg-4">
        <div class="wp-card h-100">
            <div class="wp-card-header">
                <i class="bi bi-bell text-primary"></i><strong>Latest notifications</strong>
                <a class="ms-auto small" href="${pageContext.request.contextPath}/notifications">View all</a>
            </div>
            <div class="wp-card-body p-0">
                <c:choose>
                    <c:when test="${empty recentNotifications}">
                        <div class="wp-empty">
                            <i class="bi bi-bell-slash"></i>
                            <p class="mb-0">No notifications yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-group list-group-flush">
                            <c:forEach items="${recentNotifications}" var="note">
                                <li class="list-group-item d-flex gap-2">
                                    <i class="bi ${note.notificationType.icon} text-${note.notificationType.color} mt-1"></i>
                                    <div class="min-width-0">
                                        <div class="fw-semibold small"><c:out value="${note.title}"/></div>
                                        <div class="small text-muted-soft"><c:out value="${wp:shorten(note.message, 90)}"/></div>
                                        <div class="small text-muted-soft"><c:out value="${note.relativeTime}"/></div>
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

<!-- ============================ activity log ============================ -->
<div class="wp-card mt-3">
    <div class="wp-card-header">
        <i class="bi bi-activity text-primary"></i><strong>Recent activity</strong>
        <c:if test="${currentUser.admin}">
            <a class="ms-auto small" href="${pageContext.request.contextPath}/admin/activity">Full audit log</a>
        </c:if>
    </div>
    <div class="wp-card-body">
        <c:choose>
            <c:when test="${empty recentActivity}">
                <div class="wp-empty">
                    <i class="bi bi-activity"></i>
                    <p class="mb-0">No activity recorded yet.</p>
                </div>
            </c:when>
            <c:otherwise>
                <ul class="wp-timeline">
                    <c:forEach items="${recentActivity}" var="log" end="7">
                        <li>
                            <div class="d-flex flex-wrap gap-2">
                                <i class="bi ${log.iconClass} text-primary"></i>
                                <strong class="small"><c:out value="${log.actionLabel}"/></strong>
                                <span class="small text-muted-soft">
                                    by <c:out value="${log.actorName}"/>
                                </span>
                                <span class="wp-timeline-time ms-auto">
                                    <c:out value="${wp:timeAgo(log.createdAt)}"/>
                                </span>
                            </div>
                            <div class="small text-muted-soft"><c:out value="${log.details}"/></div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

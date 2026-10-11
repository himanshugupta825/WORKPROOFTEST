<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="My profile"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
    <div class="d-flex align-items-center gap-3">
        <span class="wp-avatar" style="width:56px;height:56px;font-size:1.3rem;">
            <c:out value="${wp:initials(user.fullName)}"/>
        </span>
        <div>
            <h1 class="wp-page-title mb-0"><c:out value="${user.fullName}"/></h1>
            <div class="small text-muted-soft">
                <c:out value="${user.email}"/>
                <c:if test="${not empty user.designation}"> &middot; <c:out value="${user.designation}"/></c:if>
                <c:if test="${not empty user.department}"> &middot; <c:out value="${user.department}"/></c:if>
            </div>
            <span class="wp-badge badge-status-primary mt-1"><c:out value="${user.role.label}"/></span>
            <span class="wp-badge badge-status-${user.userStatus.badgeColor} mt-1">
                <c:out value="${user.userStatus.label}"/>
            </span>
        </div>
    </div>
    <div class="small text-muted-soft text-end">
        <div>Joined <c:out value="${wp:dateTime(user.createdAt)}"/></div>
        <div>Last sign-in: <c:out value="${wp:dateTime(user.lastLoginAt)}"/></div>
    </div>
</div>

<!-- ============================ work summary ============================ -->
<div class="row g-3 mb-3">
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Open tasks</div>
                    <div class="wp-metric-value"><c:out value="${assignedCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-primary"><i class="bi bi-list-check"></i></div>
            </div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Evidence approved</div>
                    <div class="wp-metric-value text-success"><c:out value="${approvedCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-success"><i class="bi bi-patch-check"></i></div>
            </div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Awaiting review</div>
                    <div class="wp-metric-value text-warning"><c:out value="${pendingCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-warning"><i class="bi bi-hourglass-split"></i></div>
            </div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body">
            <div class="wp-metric">
                <div>
                    <div class="wp-metric-label">Needs rework</div>
                    <div class="wp-metric-value text-danger"><c:out value="${rejectedCount}"/></div>
                </div>
                <div class="wp-metric-icon wp-icon-danger"><i class="bi bi-arrow-repeat"></i></div>
            </div>
        </div></div>
    </div>
</div>

<div class="row g-3">
    <!-- ============================ details form ============================ -->
    <div class="col-lg-7">
        <div class="wp-card mb-3">
            <div class="wp-card-header"><i class="bi bi-person-gear text-primary"></i><strong>Profile details</strong></div>
            <div class="wp-card-body">
                <form method="post" action="${pageContext.request.contextPath}/profile" novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="update">

                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label wp-required" for="fullName">Full name</label>
                            <input type="text" class="form-control" id="fullName" name="fullName" required
                                   maxlength="120" value="<c:out value='${user.fullName}'/>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="department">Department</label>
                            <input type="text" class="form-control" id="department" name="department" maxlength="100"
                                   value="<c:out value='${user.department}'/>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="designation">Designation</label>
                            <input type="text" class="form-control" id="designation" name="designation" maxlength="100"
                                   value="<c:out value='${user.designation}'/>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="phone">Phone</label>
                            <input type="text" class="form-control" id="phone" name="phone" maxlength="20"
                                   value="<c:out value='${user.phone}'/>">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="emailReadonly">Email (cannot be changed)</label>
                            <input type="email" class="form-control" id="emailReadonly"
                                   value="<c:out value='${user.email}'/>" readonly disabled>
                            <div class="form-text">Ask an administrator if you need a different email.</div>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary mt-3">
                        <i class="bi bi-check2 me-1"></i>Save profile
                    </button>
                </form>
            </div>
        </div>

        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-shield-lock text-primary"></i><strong>Change password</strong></div>
            <div class="wp-card-body">
                <form method="post" action="${pageContext.request.contextPath}/profile" novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="password">

                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label wp-required" for="currentPassword">Current password</label>
                            <input type="password" class="form-control" id="currentPassword" name="currentPassword"
                                   required autocomplete="current-password">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label wp-required" for="newPassword">New password</label>
                            <input type="password" class="form-control" id="newPassword" name="newPassword" required
                                   minlength="8" autocomplete="new-password"
                                   data-password-strength="newPasswordStrength">
                            <div class="progress mt-2" style="height:6px;">
                                <div class="progress-bar bg-danger" id="newPasswordStrength" style="width:16%"></div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label wp-required" for="confirmPassword">Confirm new password</label>
                            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                                   required autocomplete="new-password">
                        </div>
                    </div>

                    <p class="form-text mt-2 mb-0">
                        Passwords are stored as BCrypt hashes - nobody, including administrators, can read them.
                    </p>

                    <button type="submit" class="btn btn-outline-primary mt-3">
                        <i class="bi bi-key me-1"></i>Change password
                    </button>
                </form>
            </div>
        </div>
    </div>

    <!-- ============================== my work ============================== -->
    <div class="col-lg-5">
        <div class="wp-card mb-3">
            <div class="wp-card-header">
                <i class="bi bi-list-check text-primary"></i><strong>My open tasks</strong>
                <a class="ms-auto small" href="${pageContext.request.contextPath}/tasks?scope=mine">View all</a>
            </div>
            <c:choose>
                <c:when test="${empty myTasks}">
                    <div class="wp-empty py-4">
                        <i class="bi bi-check2-all"></i>
                        <p class="mb-0">No open tasks. Everything assigned to you is finished.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <ul class="list-group list-group-flush">
                        <c:forEach items="${myTasks}" var="task">
                            <li class="list-group-item d-flex justify-content-between align-items-start gap-2">
                                <div class="min-width-0">
                                    <a class="wp-cell-title d-block text-truncate"
                                       href="${pageContext.request.contextPath}/tasks/view?id=${task.id}">
                                        <c:out value="${wp:shorten(task.title, 44)}"/>
                                    </a>
                                    <span class="small text-muted-soft">
                                        <c:out value="${task.projectCode}"/> &middot;
                                        due <c:out value="${wp:date(task.dueDate)}"/>
                                    </span>
                                </div>
                                <span class="wp-badge badge-status-${task.taskStatus.badgeColor}">
                                    <c:out value="${task.taskStatus.label}"/>
                                </span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="wp-card">
            <div class="wp-card-header"><i class="bi bi-clock-history text-primary"></i><strong>My recent activity</strong></div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty myActivity}">
                        <p class="small text-muted-soft mb-0">No activity recorded for your account yet.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="wp-timeline">
                            <c:forEach items="${myActivity}" var="log">
                                <li>
                                    <div class="d-flex gap-2">
                                        <i class="bi ${log.iconClass} text-primary"></i>
                                        <strong class="small"><c:out value="${log.actionLabel}"/></strong>
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
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="User management"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
    <div>
        <h1 class="wp-page-title">User management</h1>
        <p class="wp-breadcrumb mb-0">
            Manage accounts, roles and access. Deactivated users cannot sign in but keep all their history.
        </p>
    </div>
    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/admin/activity">
        <i class="bi bi-shield-check me-1"></i>System activity
    </a>
</div>

<!-- ============================ summary cards ============================ -->
<div class="row g-3 mb-3">
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body py-3">
            <div class="wp-metric-label">Total accounts</div>
            <div class="fs-4 fw-bold"><c:out value="${totalUsers}"/></div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body py-3">
            <div class="wp-metric-label">Active accounts</div>
            <div class="fs-4 fw-bold text-success"><c:out value="${activeUsers}"/></div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body py-3">
            <div class="wp-metric-label">Managers / Members</div>
            <div class="fs-4 fw-bold"><c:out value="${managerCount}"/> / <c:out value="${memberCount}"/></div>
        </div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="wp-card"><div class="wp-card-body py-3">
            <div class="wp-metric-label">Reviewers / Admins</div>
            <div class="fs-4 fw-bold"><c:out value="${reviewerCount}"/> / <c:out value="${adminCount}"/></div>
        </div></div>
    </div>
</div>

<div class="row g-3">
    <!-- ============================== user list ============================== -->
    <div class="col-xl-8">
        <div class="wp-card">
            <div class="wp-card-header">
                <i class="bi bi-people text-primary"></i><strong>Accounts</strong>
            </div>

            <div class="wp-card-body pb-2">
                <form method="get" action="${pageContext.request.contextPath}/admin/users"
                      class="row g-2 align-items-end">
                    <div class="col-md-5">
                        <label class="form-label" for="search">Search</label>
                        <input type="search" class="form-control" id="search" name="search"
                               value="<c:out value='${search}'/>" placeholder="Name or email">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label" for="role">Role</label>
                        <select class="form-select" id="role" name="role">
                            <option value="ALL" ${role eq 'ALL' ? 'selected' : ''}>All roles</option>
                            <c:forEach items="${roles}" var="roleOption">
                                <option value="${roleOption.name}" ${role eq roleOption.name ? 'selected' : ''}>
                                    <c:out value="${roleOption.label}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label" for="status">Status</label>
                        <select class="form-select" id="status" name="status">
                            <option value="ALL" ${status eq 'ALL' ? 'selected' : ''}>All</option>
                            <c:forEach items="${statuses}" var="statusOption">
                                <option value="${statusOption}" ${status eq statusOption.name() ? 'selected' : ''}>
                                    <c:out value="${statusOption.label}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-2 d-flex gap-1">
                        <button class="btn btn-primary flex-grow-1"><i class="bi bi-funnel"></i></button>
                        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/admin/users">
                            <i class="bi bi-x-lg"></i>
                        </a>
                    </div>
                </form>
            </div>

            <c:choose>
                <c:when test="${empty userPage.items}">
                    <div class="wp-empty">
                        <i class="bi bi-person-x"></i>
                        <p class="mb-0">No account matches the filters.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="table wp-table align-middle mb-0">
                            <thead>
                            <tr>
                                <th>User</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Last sign-in</th>
                                <th class="text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${userPage.items}" var="row">
                                <tr>
                                    <td>
                                        <div class="d-flex align-items-center gap-2">
                                            <span class="wp-avatar wp-avatar-sm">
                                                <c:out value="${wp:initials(row.fullName)}"/>
                                            </span>
                                            <div class="min-width-0">
                                                <div class="fw-semibold small text-truncate">
                                                    <c:out value="${row.fullName}"/>
                                                    <c:if test="${row.id eq currentUser.id}">
                                                        <span class="wp-badge badge-status-primary ms-1">you</span>
                                                    </c:if>
                                                </div>
                                                <div class="small text-muted-soft text-truncate">
                                                    <c:out value="${row.email}"/>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="wp-badge badge-status-secondary">
                                            <c:out value="${row.role.label}"/>
                                        </span>
                                    </td>
                                    <td>
                                        <span class="wp-badge badge-status-${row.userStatus.badgeColor}">
                                            <c:out value="${row.userStatus.label}"/>
                                        </span>
                                    </td>
                                    <td class="small text-muted-soft">
                                        <c:out value="${wp:dateTime(row.lastLoginAt)}"/>
                                    </td>
                                    <td class="text-end">
                                        <div class="d-flex justify-content-end gap-1">
                                            <a class="btn btn-sm btn-outline-secondary"
                                               href="${pageContext.request.contextPath}/admin/users?edit=${row.id}"
                                               title="Edit this account">
                                                <i class="bi bi-pencil"></i>
                                            </a>
                                            <c:if test="${row.id ne currentUser.id}">
                                                <form method="post" action="${pageContext.request.contextPath}/admin/users/action"
                                                      class="m-0"
                                                      data-confirm="Change the status of this account?">
                                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                                    <input type="hidden" name="action" value="toggleStatus">
                                                    <input type="hidden" name="userId" value="${row.id}">
                                                    <button type="submit"
                                                            class="btn btn-sm ${row.active ? 'btn-outline-danger' : 'btn-outline-success'}"
                                                            title="${row.active ? 'Deactivate' : 'Activate'}">
                                                        <i class="bi ${row.active ? 'bi-person-slash' : 'bi-person-check'}"></i>
                                                    </button>
                                                </form>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <c:if test="${userPage.totalPages gt 1}">
            <nav class="mt-4" aria-label="User pages">
                <ul class="pagination justify-content-center">
                    <li class="page-item ${userPage.hasPrevious ? '' : 'disabled'}">
                        <a class="page-link" href="?page=${userPage.page - 1}&search=${search}&role=${role}&status=${status}">Previous</a>
                    </li>
                    <c:forEach items="${userPage.pageNumbers}" var="pageNumber">
                        <li class="page-item ${pageNumber eq userPage.page ? 'active' : ''}">
                            <a class="page-link" href="?page=${pageNumber}&search=${search}&role=${role}&status=${status}">
                                <c:out value="${pageNumber}"/>
                            </a>
                        </li>
                    </c:forEach>
                    <li class="page-item ${userPage.hasNext ? '' : 'disabled'}">
                        <a class="page-link" href="?page=${userPage.page + 1}&search=${search}&role=${role}&status=${status}">Next</a>
                    </li>
                </ul>
            </nav>
        </c:if>
    </div>

    <!-- ============================ create / edit ============================ -->
    <div class="col-xl-4">
        <div class="wp-card mb-3">
            <div class="wp-card-header">
                <i class="bi ${empty editUser ? 'bi-person-plus' : 'bi-person-gear'} text-primary"></i>
                <strong><c:out value="${empty editUser ? 'Create an account' : 'Edit account'}"/></strong>
                <c:if test="${not empty editUser}">
                    <a class="ms-auto small" href="${pageContext.request.contextPath}/admin/users">Cancel edit</a>
                </c:if>
            </div>
            <div class="wp-card-body">
                <c:choose>
                    <c:when test="${empty editUser}">
                        <form method="post" action="${pageContext.request.contextPath}/admin/users/action" novalidate>
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="create">

                            <div class="mb-2">
                                <label class="form-label wp-required" for="fullName">Full name</label>
                                <input type="text" class="form-control" id="fullName" name="fullName" required maxlength="120">
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="email">Email</label>
                                <input type="email" class="form-control" id="email" name="email" required maxlength="150">
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="role">Role</label>
                                <select class="form-select" id="role" name="role" required>
                                    <c:forEach items="${roles}" var="roleOption">
                                        <option value="${roleOption.name}"><c:out value="${roleOption.label}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="password">Temporary password</label>
                                <input type="password" class="form-control" id="password" name="password" required
                                       minlength="8" autocomplete="new-password">
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="confirmPassword">Confirm password</label>
                                <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                                       required autocomplete="new-password">
                            </div>
                            <div class="row g-2">
                                <div class="col-6">
                                    <label class="form-label" for="department">Department</label>
                                    <input type="text" class="form-control" id="department" name="department" maxlength="100">
                                </div>
                                <div class="col-6">
                                    <label class="form-label" for="designation">Designation</label>
                                    <input type="text" class="form-control" id="designation" name="designation" maxlength="100">
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="phone">Phone</label>
                                    <input type="text" class="form-control" id="phone" name="phone" maxlength="20">
                                </div>
                            </div>

                            <button type="submit" class="btn btn-primary w-100 mt-3">
                                <i class="bi bi-plus-lg me-1"></i>Create account
                            </button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${pageContext.request.contextPath}/admin/users/action" novalidate>
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="update">
                            <input type="hidden" name="userId" value="${editUser.id}">

                            <div class="mb-2">
                                <label class="form-label" for="editEmail">Email</label>
                                <input type="email" class="form-control" id="editEmail"
                                       value="<c:out value='${editUser.email}'/>" readonly disabled>
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="editFullName">Full name</label>
                                <input type="text" class="form-control" id="editFullName" name="fullName" required
                                       maxlength="120" value="<c:out value='${editUser.fullName}'/>">
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="editRole">Role</label>
                                <select class="form-select" id="editRole" name="role" required>
                                    <c:forEach items="${roles}" var="roleOption">
                                        <option value="${roleOption.name}"
                                            ${editUser.roleName eq roleOption.name ? 'selected' : ''}>
                                            <c:out value="${roleOption.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-2">
                                <label class="form-label wp-required" for="editStatus">Status</label>
                                <select class="form-select" id="editStatus" name="status" required>
                                    <c:forEach items="${statuses}" var="statusOption">
                                        <option value="${statusOption}"
                                            ${editUser.status eq statusOption.name() ? 'selected' : ''}>
                                            <c:out value="${statusOption.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="row g-2">
                                <div class="col-6">
                                    <label class="form-label" for="editDepartment">Department</label>
                                    <input type="text" class="form-control" id="editDepartment" name="department"
                                           maxlength="100" value="<c:out value='${editUser.department}'/>">
                                </div>
                                <div class="col-6">
                                    <label class="form-label" for="editDesignation">Designation</label>
                                    <input type="text" class="form-control" id="editDesignation" name="designation"
                                           maxlength="100" value="<c:out value='${editUser.designation}'/>">
                                </div>
                                <div class="col-12">
                                    <label class="form-label" for="editPhone">Phone</label>
                                    <input type="text" class="form-control" id="editPhone" name="phone" maxlength="20"
                                           value="<c:out value='${editUser.phone}'/>">
                                </div>
                            </div>

                            <button type="submit" class="btn btn-primary w-100 mt-3">
                                <i class="bi bi-check2 me-1"></i>Save changes
                            </button>
                        </form>

                        <hr>

                        <h6>Reset password</h6>
                        <form method="post" action="${pageContext.request.contextPath}/admin/users/action" novalidate
                              data-confirm="Reset this user's password? Share the new password securely.">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="resetPassword">
                            <input type="hidden" name="userId" value="${editUser.id}">
                            <div class="mb-2">
                                <label class="form-label" for="newPassword">New password</label>
                                <input type="password" class="form-control" id="newPassword" name="newPassword"
                                       required minlength="8" autocomplete="new-password">
                            </div>
                            <div class="mb-2">
                                <label class="form-label" for="confirmNewPassword">Confirm password</label>
                                <input type="password" class="form-control" id="confirmNewPassword"
                                       name="confirmPassword" required autocomplete="new-password">
                            </div>
                            <button type="submit" class="btn btn-outline-danger w-100">
                                <i class="bi bi-key me-1"></i>Reset password
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="alert alert-info small mb-0">
            <i class="bi bi-shield-lock me-1"></i>
            <strong>Rules enforced on the server:</strong>
            <ul class="mb-0 ps-3">
                <li>You cannot deactivate or downgrade your own account.</li>
                <li>The last active administrator cannot be deactivated.</li>
                <li>Passwords are always stored as BCrypt hashes and never logged.</li>
            </ul>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

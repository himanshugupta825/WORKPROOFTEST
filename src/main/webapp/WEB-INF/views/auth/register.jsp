<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Create account"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>

<div class="wp-auth-wrap" style="background:linear-gradient(160deg,#0f1d33,#17304f);">
    <div class="wp-auth-card">
        <div class="text-center mb-4">
            <h1 class="h4 fw-bold text-white mb-1">Create your WorkProof account</h1>
            <p style="color:#9fb3d1;" class="mb-0">Choose the role that matches what you do in your team.</p>
        </div>

        <div class="wp-card">
            <div class="wp-card-body p-4">
                <%@ include file="/WEB-INF/views/fragments/messages.jspf" %>

                <form method="post" action="${pageContext.request.contextPath}/register" novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">

                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label wp-required" for="fullName">Full name</label>
                            <input type="text" class="form-control" id="fullName" name="fullName" required
                                   maxlength="120" value="<c:out value='${fullName}'/>" placeholder="e.g. Aarav Sharma">
                        </div>

                        <div class="col-12">
                            <label class="form-label wp-required" for="email">Email address</label>
                            <input type="email" class="form-control" id="email" name="email" required
                                   maxlength="150" value="<c:out value='${email}'/>" placeholder="you@college.edu">
                            <div class="form-text">Used to sign in. One account per email address.</div>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label wp-required" for="password">Password</label>
                            <input type="password" class="form-control" id="password" name="password" required
                                   minlength="8" autocomplete="new-password"
                                   data-password-strength="passwordStrength" placeholder="Min. 8 characters">
                            <div class="progress mt-2" style="height:6px;">
                                <div class="progress-bar bg-danger" id="passwordStrength" style="width:16%"></div>
                            </div>
                            <div class="form-text">At least 8 characters with one letter and one digit.</div>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label wp-required" for="confirmPassword">Confirm password</label>
                            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                                   required autocomplete="new-password" placeholder="Repeat the password">
                        </div>

                        <div class="col-12">
                            <label class="form-label wp-required" for="role">Your role</label>
                            <select class="form-select" id="role" name="role" required>
                                <option value="">Choose a role...</option>
                                <c:forEach items="${roles}" var="roleOption">
                                    <option value="${roleOption}" ${role eq roleOption ? 'selected' : ''}>
                                        <c:out value="${roleOption.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">
                                <strong>Project Manager</strong> creates projects and tasks &middot;
                                <strong>Team Member</strong> works on tasks and uploads evidence &middot;
                                <strong>Reviewer</strong> verifies evidence and gives feedback.
                            </div>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label" for="department">Department</label>
                            <input type="text" class="form-control" id="department" name="department" maxlength="100"
                                   value="<c:out value='${department}'/>" placeholder="Computer Science">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="designation">Designation</label>
                            <input type="text" class="form-control" id="designation" name="designation" maxlength="100"
                                   value="<c:out value='${designation}'/>" placeholder="Student / Mentor">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="phone">Phone</label>
                            <input type="text" class="form-control" id="phone" name="phone" maxlength="20"
                                   value="<c:out value='${phone}'/>" placeholder="Optional">
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary w-100 py-2 mt-4">
                        <i class="bi bi-person-plus me-1"></i>Create account
                    </button>
                </form>

                <hr class="my-4">
                <p class="text-center mb-0 small">
                    Already registered?
                    <a href="${pageContext.request.contextPath}/login" class="fw-semibold">Sign in instead</a>
                </p>
            </div>
        </div>

        <div class="text-center mt-3 small">
            <a class="link-light" href="${pageContext.request.contextPath}/">&larr; Back to the landing page</a>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/vendor/bootstrap/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/workproof.js"></script>
</body>
</html>

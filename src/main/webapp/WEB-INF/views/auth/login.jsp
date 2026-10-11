<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Sign in"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>

<div class="wp-auth-wrap" style="background:linear-gradient(160deg,#0f1d33,#17304f);">
    <div class="wp-auth-card">
        <div class="text-center mb-4">
            <span class="wp-logo d-inline-grid mb-2" style="width:52px;height:52px;border-radius:14px;
                  background:linear-gradient(135deg,#2563eb,#0ea5e9);place-items:center;font-size:1.4rem;color:#fff;">
                <i class="bi bi-patch-check-fill"></i>
            </span>
            <h1 class="h4 fw-bold text-white mb-1">Welcome back to WorkProof</h1>
            <p style="color:#9fb3d1;" class="mb-0">Sign in to manage projects, tasks and evidence.</p>
        </div>

        <div class="wp-card">
            <div class="wp-card-body p-4">
                <%@ include file="/WEB-INF/views/fragments/messages.jspf" %>

                <form method="post" action="${pageContext.request.contextPath}/login" novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">

                    <div class="mb-3">
                        <label class="form-label wp-required" for="email">Email address</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-envelope"></i></span>
                            <input type="email" class="form-control" id="email" name="email"
                                   value="<c:out value='${email}'/>" required autofocus
                                   autocomplete="username" placeholder="you@college.edu">
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label wp-required" for="password">Password</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock"></i></span>
                            <input type="password" class="form-control" id="password" name="password"
                                   required autocomplete="current-password" placeholder="Your password">
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary w-100 py-2">
                        <i class="bi bi-box-arrow-in-right me-1"></i>Sign in
                    </button>
                </form>

                <hr class="my-4">

                <p class="text-center mb-0 small">
                    New to WorkProof?
                    <a href="${pageContext.request.contextPath}/register" class="fw-semibold">Create an account</a>
                </p>
            </div>
        </div>

        <div class="text-center mt-3 small" style="color:#9fb3d1;">
            <a class="link-light" href="${pageContext.request.contextPath}/">&larr; Back to the landing page</a>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/assets/vendor/bootstrap/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/workproof.js"></script>
</body>
</html>

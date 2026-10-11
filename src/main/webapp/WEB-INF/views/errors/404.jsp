<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Not found"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-7 text-center">
            <div class="wp-card">
                <div class="wp-card-body py-5">
                    <div class="display-1 fw-bold text-warning">404</div>
                    <h1 class="h4 mt-2 mb-2"><c:out value="${empty errorTitle ? 'Page not found' : errorTitle}"/></h1>
                    <p class="text-muted-soft">
                        <c:out value="${empty errorMessage
                                ? 'The page or record you asked for does not exist, or you followed an old link.'
                                : errorMessage}"/>
                    </p>
                    <div class="d-flex justify-content-center gap-2 mt-4">
                        <a class="btn btn-primary" href="${pageContext.request.contextPath}/dashboard">
                            <i class="bi bi-speedometer2 me-1"></i>Back to my dashboard
                        </a>
                        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/">
                            <i class="bi bi-house me-1"></i>Landing page
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

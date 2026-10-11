<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Server error"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-7 text-center">
            <div class="wp-card">
                <div class="wp-card-body py-5">
                    <div class="display-1 fw-bold text-danger">500</div>
                    <h1 class="h4 mt-2 mb-2"><c:out value="${empty errorTitle ? 'Something went wrong' : errorTitle}"/></h1>
                    <p class="text-muted-soft">
                        <c:out value="${empty errorMessage
                                ? 'The application ran into an unexpected problem.'
                                : errorMessage}"/>
                    </p>
                    <div class="alert alert-warning text-start small mt-3 mb-0">
                        <strong>What to check as a developer:</strong>
                        <ul class="mb-0 ps-3">
                            <li>Is MySQL running and is the <code>workproof</code> database reachable?</li>
                            <li>Look at the Tomcat log: <code>logs/catalina.out</code> (Linux) or
                                <code>logs\\catalina..log</code> (Windows).</li>
                            <li>Did you import <code>database/schema.sql</code> after the last schema change?</li>
                        </ul>
                    </div>
                    <a class="btn btn-primary mt-4" href="${pageContext.request.contextPath}/dashboard">
                        <i class="bi bi-speedometer2 me-1"></i>Back to my dashboard
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Access denied"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-7 text-center">
            <div class="wp-card">
                <div class="wp-card-body py-5">
                    <div class="display-1 fw-bold text-danger">403</div>
                    <h1 class="h4 mt-2 mb-2"><c:out value="${empty errorTitle ? 'Access denied' : errorTitle}"/></h1>
                    <p class="text-muted-soft">
                        <c:out value="${empty errorMessage
                                ? 'Your account does not have permission to open this page or perform this action.'
                                : errorMessage}"/>
                    </p>
                    <div class="d-flex justify-content-center gap-2 mt-4">
                        <a class="btn btn-primary" href="${pageContext.request.contextPath}/dashboard">
                            <i class="bi bi-speedometer2 me-1"></i>Back to my dashboard
                        </a>
                        <c:if test="${not empty sessionScope.currentUser}">
                            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/projects">
                                <i class="bi bi-kanban me-1"></i>My projects
                            </a>
                        </c:if>
                    </div>
                    <p class="small text-muted-soft mt-4 mb-0">
                        Permissions in WorkProof are checked on the server for every action,
                        so hiding a button is never the only protection.
                    </p>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

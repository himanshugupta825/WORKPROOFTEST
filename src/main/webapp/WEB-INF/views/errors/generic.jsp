<%--
  Generic error page used by ErrorServlet (mapped from web.xml error pages).
  It never prints a stack trace or SQL text - only a friendly message and the code.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Error"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-7 text-center">
            <div class="wp-card">
                <div class="wp-card-body py-5">
                    <div class="display-1 fw-bold text-danger">
                        <c:out value="${empty errorCode ? 500 : errorCode}"/>
                    </div>
                    <h1 class="h4 mt-2 mb-2"><c:out value="${errorTitle}"/></h1>
                    <p class="text-muted-soft"><c:out value="${errorMessage}"/></p>

                    <c:choose>
                        <c:when test="${not empty sessionScope.currentUser}">
                            <div class="d-flex justify-content-center gap-2 mt-3">
                                <a class="btn btn-primary" href="${pageContext.request.contextPath}/dashboard">
                                    <i class="bi bi-speedometer2 me-1"></i>Back to my dashboard
                                </a>
                                <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/projects">
                                    <i class="bi bi-kanban me-1"></i>Projects
                                </a>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <a class="btn btn-primary mt-3" href="${pageContext.request.contextPath}/login">
                                <i class="bi bi-box-arrow-in-right me-1"></i>Sign in
                            </a>
                        </c:otherwise>
                    </c:choose>

                    <p class="small text-muted-soft mt-4 mb-0">
                        Error reference: <code><c:out value="${errorCode}"/></code> &middot;
                        request path: <code><c:out value="${requestScope['javax.servlet.error.request_uri']}"/></code>
                    </p>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

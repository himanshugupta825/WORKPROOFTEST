<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit project' : 'Create project'}"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/projects">Projects</a></li>
        <li class="breadcrumb-item active" aria-current="page">
            <c:out value="${mode eq 'edit' ? 'Edit project' : 'New project'}"/>
        </li>
    </ol>
</nav>

<h1 class="wp-page-title mb-3">
    <c:out value="${mode eq 'edit' ? 'Edit project' : 'Create a new project'}"/>
</h1>

<div class="row">
    <div class="col-lg-9 col-xl-7">
        <div class="wp-card">
            <div class="wp-card-body">
                <form method="post"
                      action="${pageContext.request.contextPath}/projects/${mode eq 'edit' ? 'edit' : 'new'}"
                      novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <c:if test="${mode eq 'edit'}">
                        <input type="hidden" name="id" value="${project.id}">
                    </c:if>

                    <div class="row g-3">
                        <div class="col-md-8">
                            <label class="form-label wp-required" for="name">Project name</label>
                            <input type="text" class="form-control" id="name" name="name" required maxlength="150"
                                   value="<c:out value='${project.name}'/>" placeholder="e.g. Complaint Management Portal">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label wp-required" for="code">Project code</label>
                            <input type="text" class="form-control text-uppercase" id="code" name="code" required
                                   maxlength="20" value="<c:out value='${project.code}'/>" placeholder="CMP-2026">
                            <div class="form-text">Letters, digits and hyphens. Must be unique.</div>
                        </div>

                        <div class="col-12">
                            <label class="form-label wp-required" for="description">Description</label>
                            <textarea class="form-control" id="description" name="description" rows="5" required
                                      maxlength="5000"
                                      placeholder="What is this project about? What will be delivered?"><c:out value="${project.description}"/></textarea>
                            <div class="form-text">At least 10 characters.</div>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label wp-required" for="startDate">Start date</label>
                            <input type="date" class="form-control" id="startDate" name="startDate" required
                                   value="${project.startDate}">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label wp-required" for="endDate">End date</label>
                            <input type="date" class="form-control" id="endDate" name="endDate" required
                                   value="${project.endDate}">
                            <div class="form-text">Cannot be before the start date.</div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="status">Status</label>
                            <select class="form-select" id="status" name="status">
                                <c:forEach items="${statuses}" var="statusOption">
                                    <option value="${statusOption}"
                                        ${project.status eq statusOption.name() ? 'selected' : ''}>
                                        <c:out value="${statusOption.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">New projects usually start as <em>Planned</em> or <em>Active</em>.</div>
                        </div>
                    </div>

                    <div class="d-flex gap-2 mt-4">
                        <button type="submit" class="btn btn-primary px-4">
                            <i class="bi bi-check2 me-1"></i>
                            <c:out value="${mode eq 'edit' ? 'Save changes' : 'Create project'}"/>
                        </button>
                        <a class="btn btn-outline-secondary"
                           href="${mode eq 'edit'
                                   ? pageContext.request.contextPath.concat('/projects/view?id=').concat(project.id)
                                   : pageContext.request.contextPath.concat('/projects')}">Cancel</a>
                    </div>
                </form>
            </div>
        </div>

        <div class="alert alert-info mt-3 small mb-0">
            <i class="bi bi-info-circle me-1"></i>
            You automatically become the project manager and owner of a new project. Add members and assign
            tasks from the project page.
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

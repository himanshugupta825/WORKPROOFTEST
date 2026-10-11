<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="${mode eq 'edit' ? 'Edit task' : 'New task'}"/>
<%@ include file="/WEB-INF/views/fragments/head.jspf" %>
<%@ include file="/WEB-INF/views/fragments/shell-open.jspf" %>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb small">
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/tasks">Tasks</a></li>
        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/projects/view?id=${project.id}">
            <c:out value="${project.code}"/></a></li>
        <li class="breadcrumb-item active" aria-current="page">
            <c:out value="${mode eq 'edit' ? 'Edit task' : 'New task'}"/>
        </li>
    </ol>
</nav>

<h1 class="wp-page-title mb-1"><c:out value="${mode eq 'edit' ? 'Edit task' : 'Create a task'}"/></h1>
<p class="wp-breadcrumb mb-4">Project: <c:out value="${project.name}"/></p>

<div class="row">
    <div class="col-lg-9 col-xl-8">
        <div class="wp-card">
            <div class="wp-card-body">
                <form method="post"
                      action="${pageContext.request.contextPath}/tasks/${mode eq 'edit' ? 'edit' : 'new'}"
                      novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="projectId" value="${project.id}">
                    <c:if test="${mode eq 'edit'}">
                        <input type="hidden" name="id" value="${task.id}">
                    </c:if>

                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label wp-required" for="title">Task title</label>
                            <input type="text" class="form-control" id="title" name="title" required maxlength="180"
                                   value="<c:out value='${task.title}'/>"
                                   placeholder="e.g. Design the login and registration screens">
                        </div>

                        <div class="col-12">
                            <label class="form-label wp-required" for="description">Description</label>
                            <textarea class="form-control" id="description" name="description" rows="5" required
                                      maxlength="5000"
                                      placeholder="What exactly must be delivered? Include acceptance criteria so the reviewer can verify it."><c:out value="${task.description}"/></textarea>
                        </div>

                        <div class="col-md-4">
                            <label class="form-label wp-required" for="priority">Priority</label>
                            <select class="form-select" id="priority" name="priority" required>
                                <c:forEach items="${priorities}" var="priorityOption">
                                    <option value="${priorityOption}"
                                        ${task.priority eq priorityOption.name() ? 'selected' : ''}>
                                        <c:out value="${priorityOption.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="startDate">Start date</label>
                            <input type="date" class="form-control" id="startDate" name="startDate"
                                   value="${task.startDate}">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label wp-required" for="dueDate">Deadline</label>
                            <input type="date" class="form-control" id="dueDate" name="dueDate" required
                                   value="${task.dueDate}">
                        </div>

                        <div class="col-12">
                            <label class="form-label" for="assigneeIds">Assign to project members</label>
                            <select class="form-select" id="assigneeIds" name="assigneeIds" multiple size="6">
                                <c:forEach items="${members}" var="member">
                                    <option value="${member.userId}"
                                        ${assigneeIds.contains(member.userId) ? 'selected' : ''}>
                                        <c:out value="${member.fullName}"/>
                                        (<c:out value="${member.email}"/>) &mdash;
                                        <c:out value="${member.projectRole.label}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <div class="form-text">
                                Hold Ctrl (Windows/Linux) or Cmd (macOS) to select more than one person.
                                Only active project members can be assigned.
                            </div>
                        </div>
                    </div>

                    <div class="d-flex gap-2 mt-4">
                        <button type="submit" class="btn btn-primary px-4">
                            <i class="bi bi-check2 me-1"></i>
                            <c:out value="${mode eq 'edit' ? 'Save changes' : 'Create task'}"/>
                        </button>
                        <a class="btn btn-outline-secondary"
                           href="${mode eq 'edit'
                                   ? pageContext.request.contextPath.concat('/tasks/view?id=').concat(task.id)
                                   : pageContext.request.contextPath.concat('/projects/view?id=').concat(project.id)}">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/fragments/foot.jspf" %>

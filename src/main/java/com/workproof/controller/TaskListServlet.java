package com.workproof.controller;

import com.workproof.model.PagedResult;
import com.workproof.model.Project;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.ProjectService;
import com.workproof.service.TaskService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Task listing with search, status/priority filters, "only my tasks" scope,
 * project filter, sorting and pagination.
 */
@WebServlet(name = "TaskListServlet", urlPatterns = {"/tasks"})
public class TaskListServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 10;

    private final TaskService taskService = new TaskService();
    private final ProjectService projectService = new ProjectService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        Integer projectId = intParam(request, "projectId");
        String status = param(request, "status");
        String priority = param(request, "priority");
        String search = param(request, "search");
        // A team member starts on "my tasks"; managers and reviewers see everything.
        String scope = ValidationUtil.trimToNull(param(request, "scope"));
        if (scope == null) {
            scope = user.isMember() ? "mine" : "all";
        }
        String sortKey = ValidationUtil.trimToNull(param(request, "sort"));
        String sortDir = ValidationUtil.trimToNull(param(request, "dir"));
        int page = WebUtil.pageParam(request);

        PagedResult<Task> tasks = taskService.list(user, projectId, status, priority, search, scope,
                sortKey, sortDir, page, PAGE_SIZE);

        List<Project> projects = projectService.findProjectsForUser(user);

        request.setAttribute("taskPage", tasks);
        request.setAttribute("projects", projects);
        request.setAttribute("statuses", taskService.allStatuses());
        request.setAttribute("priorities", taskService.allPriorities());
        request.setAttribute("selectedProjectId", projectId);
        request.setAttribute("status", ValidationUtil.trimToNull(status) == null ? "ALL" : status);
        request.setAttribute("priority", ValidationUtil.trimToNull(priority) == null ? "ALL" : priority);
        request.setAttribute("search", search);
        request.setAttribute("scope", scope);
        request.setAttribute("sort", sortKey == null ? "due" : sortKey);
        request.setAttribute("dir", "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc");
        render(request, response, "tasks/list.jsp");
    }
}

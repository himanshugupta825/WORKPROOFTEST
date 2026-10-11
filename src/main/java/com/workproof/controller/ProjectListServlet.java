package com.workproof.controller;

import com.workproof.model.PagedResult;
import com.workproof.model.Project;
import com.workproof.model.User;
import com.workproof.service.ProjectService;
import com.workproof.util.ValidationUtil;
import com.workproof.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Project listing with search, status filter, archive toggle and pagination.
 * A team member only sees projects they belong to; an administrator sees all.
 */
@WebServlet(name = "ProjectListServlet", urlPatterns = {"/projects"})
public class ProjectListServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 9;

    private final ProjectService projectService = new ProjectService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        String search = param(request, "search");
        String status = param(request, "status");
        boolean includeArchived = WebUtil.boolParam(request, "archived");
        int page = WebUtil.pageParam(request);

        PagedResult<Project> projects = projectService.listForViewer(user, search, status,
                includeArchived, page, PAGE_SIZE);

        request.setAttribute("projectPage", projects);
        request.setAttribute("search", search);
        request.setAttribute("status", ValidationUtil.trimToNull(status) == null ? "ALL" : status);
        request.setAttribute("archived", includeArchived);
        request.setAttribute("statuses", projectService.allStatuses());
        render(request, response, "projects/list.jsp");
    }
}

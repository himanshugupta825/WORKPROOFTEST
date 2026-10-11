package com.workproof.controller;

import com.workproof.model.Project;
import com.workproof.model.ProjectMember;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.ActivityService;
import com.workproof.service.Permissions;
import com.workproof.service.ProjectService;
import com.workproof.service.SubmissionService;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Project details: description, members, tasks, progress and recent activity.
 * Access is checked by the service (members and admins only).
 */
@WebServlet(name = "ProjectViewServlet", urlPatterns = {"/projects/view"})
public class ProjectViewServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private final SubmissionService submissionService = new SubmissionService();
    private final ActivityService activityService = new ActivityService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int projectId = requiredIntParam(request, "id", "Project");

        try {
            Project project = projectService.getForViewer(user, projectId);
            List<ProjectMember> members = projectService.findMembers(projectId);
            List<Task> tasks = projectService.findTasks(projectId);

            request.setAttribute("project", project);
            request.setAttribute("members", members);
            request.setAttribute("tasks", tasks);
            request.setAttribute("candidates", projectService.findCandidateUsers(projectId));
            request.setAttribute("projectRoles", projectService.assignableRoles());
            request.setAttribute("canManage", Permissions.canManageProject(user, project,
                    project.getCurrentUserProjectRole()));
            request.setAttribute("canReview", Permissions.canReviewInProject(user,
                    project.getCurrentUserProjectRole()));
            request.setAttribute("upcomingDeadlines", projectService.findUpcomingDeadlines(projectId, 5));
            request.setAttribute("activity", activityService.recentForProject(projectId, 8));
            request.setAttribute("taskStatuses", taskService.allStatuses());
            request.setAttribute("submissionStatuses", submissionService.allStatuses());

            render(request, response, "projects/view.jsp");
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

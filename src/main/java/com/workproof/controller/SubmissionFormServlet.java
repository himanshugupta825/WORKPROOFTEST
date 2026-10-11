package com.workproof.controller;

import com.workproof.exception.ValidationException;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.service.SubmissionService;
import com.workproof.service.TaskService;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Proof-of-work submission form (Module 5).
 *
 * The @MultipartConfig annotation switches this servlet into multipart mode, which is
 * what makes request.getPart() work. The size limit here is the HARD limit enforced by
 * the container (harder than the friendly check inside FileStorageUtil).
 */
@WebServlet(name = "SubmissionFormServlet", urlPatterns = {"/submissions/new"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,          // 1 MB: keep small files in memory
        maxFileSize = 10L * 1024L * 1024L,        // 10 MB per file
        maxRequestSize = 60L * 1024L * 1024L)     // 6 files x 10 MB + form fields
public class SubmissionFormServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final SubmissionService submissionService = new SubmissionService();
    private final TaskService taskService = new TaskService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int taskId = requiredIntParam(request, "taskId", "Task");

        try {
            Task task = taskService.getForViewer(user, taskId);
            request.setAttribute("task", task);
            request.setAttribute("history", submissionService.historyForTask(user, task));
            request.setAttribute("acceptedTypes", submissionService.acceptedFileTypes());
            request.setAttribute("maxFileSize", submissionService.maxFileSizeLabel());
            request.setAttribute("maxFiles", SubmissionService.MAX_FILES_PER_SUBMISSION);
            render(request, response, "submissions/form.jsp");
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        Integer taskId = intParam(request, "taskId");

        if (taskId == null) {
            redirectWithError(request, response, "/tasks", "Please choose a task before submitting evidence.");
            return;
        }

        try {
            List<Part> parts = new ArrayList<>(request.getParts());
            int submissionId = submissionService.submit(user, taskId, param(request, "title"),
                    param(request, "description"), parts, clientIp(request));

            redirectWithSuccess(request, response, "/submissions/view?id=" + submissionId,
                    "Evidence submitted. Your reviewer has been notified.");

        } catch (ValidationException e) {
            // Show the form again with everything the user typed, including the error.
            Task task = taskService.getForViewer(user, taskId);
            request.setAttribute("task", task);
            request.setAttribute("formError", e.getMessage());
            request.setAttribute("formTitle", param(request, "title"));
            request.setAttribute("formDescription", param(request, "description"));
            request.setAttribute("history", submissionService.historyForTask(user, task));
            request.setAttribute("acceptedTypes", submissionService.acceptedFileTypes());
            request.setAttribute("maxFileSize", submissionService.maxFileSizeLabel());
            request.setAttribute("maxFiles", SubmissionService.MAX_FILES_PER_SUBMISSION);
            render(request, response, "submissions/form.jsp");
        } catch (IllegalStateException e) {
            // Thrown by the container when a file exceeds maxFileSize.
            request.setAttribute("formError", "One of the files is too large. The maximum size is "
                    + submissionService.maxFileSizeLabel() + " per file.");
            doGet(request, response);
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

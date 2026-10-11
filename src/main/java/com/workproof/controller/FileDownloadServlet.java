package com.workproof.controller;

import com.workproof.model.SubmissionFile;
import com.workproof.model.User;
import com.workproof.service.SubmissionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Authorised evidence download.
 *
 * The file never lives inside the web application, so this servlet is the ONLY way to
 * read it. The service checks that the requester is the submitter, a member of the
 * project, or an administrator, and writes an audit entry for every download.
 *
 * The response is always sent as an attachment with a generic content type, so a
 * malicious HTML/SVG file can never execute in the browser context of the app.
 */
@WebServlet(name = "FileDownloadServlet", urlPatterns = {"/submissions/download"})
public class FileDownloadServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final SubmissionService submissionService = new SubmissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int fileId = requiredIntParam(request, "id", "File");

        try {
            SubmissionService.DownloadResult result = submissionService.openForDownload(user, fileId,
                    clientIp(request));
            SubmissionFile file = result.getFile();

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + file.getOriginalName().replace("\"", "") + "\"");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLengthLong(Files.size(result.getPath()));

            try (OutputStream out = response.getOutputStream()) {
                Files.copy(result.getPath(), out);
                out.flush();
            }
        } catch (RuntimeException e) {
            handleServiceException(request, response, e, true);
        }
    }
}

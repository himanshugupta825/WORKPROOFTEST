package com.workproof.controller;

import com.workproof.service.EvidenceService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
@RequestMapping("/evidence/files")
public class EvidenceFileController {

    private final EvidenceService evidenceService;

    public EvidenceFileController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable("filename") String filename) {
        Resource file = evidenceService.loadAsResource(filename);

        String contentType = "application/octet-stream";
        try {
            String probed = Files.probeContentType(Paths.get(file.getURI()));
            if (probed != null) {
                contentType = probed;
            }
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}


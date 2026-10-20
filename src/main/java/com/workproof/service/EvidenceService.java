package com.workproof.service;

import com.workproof.exception.ValidationException;
import com.workproof.model.Evidence;
import com.workproof.model.Job;
import com.workproof.model.User;
import com.workproof.model.enums.AuditAction;
import com.workproof.model.enums.EvidenceType;
import com.workproof.repository.EvidenceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final AuditService auditService;

    @Value("${workproof.upload.dir:uploads}")
    private String uploadDirStr;

    @Value("${workproof.upload.max-file-size-mb:10}")
    private int maxFileSizeMb;

    private Path rootUploadPath;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "application/pdf", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public EvidenceService(EvidenceRepository evidenceRepository, AuditService auditService) {
        this.evidenceRepository = evidenceRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        this.rootUploadPath = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootUploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directory: " + rootUploadPath, e);
        }
    }

    @Transactional
    public Evidence storeEvidence(Job job, EvidenceType type, String caption, MultipartFile file, User uploader, String ip) {
        if (type == null) {
            throw new ValidationException("Evidence type is required");
        }

        if (type == EvidenceType.NOTE) {
            if (caption == null || caption.trim().isEmpty()) {
                throw new ValidationException("Note content cannot be empty");
            }
            Evidence noteEvidence = new Evidence();
            noteEvidence.setJob(job);
            noteEvidence.setEvidenceType(type);
            noteEvidence.setCaption(caption);
            noteEvidence.setNoteContent(caption);
            noteEvidence.setStoredFileName("note.txt");
            noteEvidence.setOriginalFileName("note.txt");
            noteEvidence.setFileSize(0L);
            noteEvidence.setContentType("text/plain");
            noteEvidence.setUploadedBy(uploader);
            noteEvidence.setUploadedAt(LocalDateTime.now());

            Evidence saved = evidenceRepository.save(noteEvidence);
            auditService.log(AuditAction.EVIDENCE_UPLOADED, job.getId(), job.getTitle(), uploader,
                    "Added note evidence: " + (caption.length() > 50 ? caption.substring(0, 47) + "..." : caption), ip);
            return saved;
        }

        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required for evidence upload");
        }

        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ValidationException("File size exceeds maximum allowed limit of " + maxFileSizeMb + " MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "file_" + System.currentTimeMillis();
        }

        String contentType = file.getContentType();
        String fileExt = extractExtension(originalFilename);

        if (type == EvidenceType.BEFORE_PHOTO || type == EvidenceType.AFTER_PHOTO) {
            if (contentType != null && !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                if (!isAllowedImageExtension(fileExt)) {
                    throw new ValidationException("Invalid photo format. Only JPG, PNG, WEBP, and GIF are allowed.");
                }
            }
        }

        // Generate safe, unguessable stored filename
        String safeStoredFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + fileExt;

        try {
            Path destinationFile = this.rootUploadPath.resolve(safeStoredFileName).normalize();
            // Security check against directory traversal
            if (!destinationFile.getParent().equals(this.rootUploadPath)) {
                throw new ValidationException("Cannot store file outside current directory");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Evidence evidence = new Evidence();
            evidence.setJob(job);
            evidence.setEvidenceType(type);
            evidence.setCaption(caption);
            evidence.setStoredFileName(safeStoredFileName);
            evidence.setOriginalFileName(originalFilename);
            evidence.setFileSize(file.getSize());
            evidence.setContentType(contentType != null ? contentType : "application/octet-stream");
            evidence.setUploadedBy(uploader);
            evidence.setUploadedAt(LocalDateTime.now());

            Evidence saved = evidenceRepository.save(evidence);
            auditService.log(AuditAction.EVIDENCE_UPLOADED, job.getId(), job.getTitle(), uploader,
                    "Uploaded " + type.getLabel() + ": " + originalFilename + " (" + file.getSize() + " bytes)", ip);
            return saved;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = this.rootUploadPath.resolve(filename).normalize();
            if (!file.startsWith(this.rootUploadPath)) {
                throw new ValidationException("Access denied: invalid file path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ValidationException("File not found or not readable: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ValidationException("Invalid file URL: " + filename, e);
        }
    }

    public List<Evidence> getEvidenceForJob(Job job) {
        return evidenceRepository.findByJobOrderByUploadedAtAsc(job);
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            return filename.substring(dot).toLowerCase();
        }
        return "";
    }

    private boolean isAllowedImageExtension(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) ||
               ".webp".equals(ext) || ".gif".equals(ext);
    }
}


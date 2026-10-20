package com.workproof.model;

import com.workproof.model.enums.EvidenceType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;

    @Column(name = "caption")
    private String caption;

    @Column(name = "stored_file_name")
    private String storedFileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "note_content", columnDefinition = "TEXT")
    private String noteContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Evidence() {}

    public Evidence(Job job, EvidenceType evidenceType, String caption, String storedFileName, String originalFileName, Long fileSize, String contentType, User uploadedBy) {
        this.job = job;
        this.evidenceType = evidenceType;
        this.caption = caption;
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public EvidenceType getEvidenceType() { return evidenceType; }
    public void setEvidenceType(EvidenceType evidenceType) { this.evidenceType = evidenceType; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getNoteContent() { return noteContent; }
    public void setNoteContent(String noteContent) { this.noteContent = noteContent; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isImage() {
        return (contentType != null && contentType.startsWith("image/")) ||
               (originalFileName != null && (originalFileName.toLowerCase().endsWith(".png") ||
                                             originalFileName.toLowerCase().endsWith(".jpg") ||
                                             originalFileName.toLowerCase().endsWith(".jpeg") ||
                                             originalFileName.toLowerCase().endsWith(".webp")));
    }

    public boolean isBeforePhoto() {
        return evidenceType == EvidenceType.BEFORE_PHOTO;
    }

    public boolean isAfterPhoto() {
        return evidenceType == EvidenceType.AFTER_PHOTO;
    }
}


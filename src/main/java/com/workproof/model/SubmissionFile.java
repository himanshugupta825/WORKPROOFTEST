package com.workproof.model;

import com.workproof.util.FileStorageUtil;

import java.time.LocalDateTime;

/**
 * Row of `submission_files`. The binary content lives on disk; this object holds
 * only metadata, as required by the spec ("store file metadata in MySQL").
 */
public class SubmissionFile {

    private int id;
    private int submissionId;
    private String originalName;
    private String storedName;
    private String contentType;
    private long fileSize;
    private String sha256;
    private LocalDateTime uploadedAt;

    /** Relative path on disk: <yyyy>/<MM>/<storedName>. Kept out of the DB for simplicity. */
    private String relativeLocation;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getRelativeLocation() { return relativeLocation; }
    public void setRelativeLocation(String relativeLocation) { this.relativeLocation = relativeLocation; }

    /* ------------------------------- derived from stored_name ------------------------------- */

    /** Files are stored under <uploadRoot>/<yyyy>/<MM>/<storedName>, so the path is derivable. */
    public String getDerivedRelativeLocation() {
        if (uploadedAt != null && storedName != null) {
            return String.format("%04d/%02d/%s", uploadedAt.getYear(), uploadedAt.getMonthValue(), storedName);
        }
        return relativeLocation;
    }

    public String getHumanSize() {
        return FileStorageUtil.humanReadableSize(fileSize);
    }

    /** Short MIME family used to pick an icon in the UI. */
    public String getFileKind() {
        String ext = FileStorageUtil.extensionOf(originalName);
        switch (ext) {
            case "png": case "jpg": case "jpeg": case "gif": case "webp":
                return "image";
            case "pdf":
                return "pdf";
            case "doc": case "docx":
                return "word";
            case "xls": case "xlsx": case "csv":
                return "excel";
            case "ppt": case "pptx":
                return "powerpoint";
            case "zip":
                return "archive";
            case "mp4":
                return "video";
            default:
                return "file";
        }
    }

    public String getIconClass() {
        switch (getFileKind()) {
            case "image":      return "bi-file-earmark-image";
            case "pdf":        return "bi-file-earmark-pdf";
            case "word":       return "bi-file-earmark-word";
            case "excel":      return "bi-file-earmark-excel";
            case "powerpoint": return "bi-file-earmark-ppt";
            case "archive":    return "bi-file-earmark-zip";
            case "video":      return "bi-file-earmark-play";
            default:           return "bi-file-earmark-text";
        }
    }

    public boolean isImage() {
        return "image".equals(getFileKind());
    }
}

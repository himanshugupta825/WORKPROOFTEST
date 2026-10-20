package com.workproof.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public class EvidenceUploadDto {

    @NotBlank(message = "Evidence type must be selected")
    private String evidenceType;

    private String caption;

    private MultipartFile file;

    public EvidenceUploadDto() {}

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}


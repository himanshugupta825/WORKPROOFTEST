package com.workproof.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerApprovalDto {

    @NotBlank(message = "Approver name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String approverName;

    private String comments;

    public CustomerApprovalDto() {}

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}


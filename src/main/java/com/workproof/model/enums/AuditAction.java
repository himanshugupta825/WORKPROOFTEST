package com.workproof.model.enums;

public enum AuditAction {
    JOB_CREATED("Job Created", "success"),
    JOB_UPDATED("Job Updated", "info"),
    JOB_ASSIGNED("Job Assigned", "primary"),
    JOB_STARTED("Job Started", "warning"),
    EVIDENCE_UPLOADED("Evidence Uploaded", "secondary"),
    JOB_COMPLETED("Job Completed", "primary"),
    CUSTOMER_APPROVED("Customer Approved", "success"),
    PROOF_GENERATED("Proof Generated", "dark");

    private final String label;
    private final String badgeClass;

    AuditAction(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}

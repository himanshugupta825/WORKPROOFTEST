package com.workproof.model.enums;

/** Proof submission state (proof_submissions.status). */
public enum SubmissionStatus {
    PENDING("Pending Review", "warning"),
    APPROVED("Approved", "success"),
    REJECTED("Rejected", "danger"),
    SUPERSEDED("Superseded", "secondary");

    private final String label;
    private final String badgeColor;

    SubmissionStatus(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static SubmissionStatus from(String value) {
        if (value == null) {
            return PENDING;
        }
        for (SubmissionStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return PENDING;
    }
}

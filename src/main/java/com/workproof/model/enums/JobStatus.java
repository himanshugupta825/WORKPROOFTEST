package com.workproof.model.enums;

import java.util.Set;

/**
 * Job lifecycle state flow:
 *   CREATED -> ASSIGNED -> IN_PROGRESS -> COMPLETED -> APPROVED (and CANCELLED)
 *
 * This enum is the single source of truth for valid status transitions.
 */
public enum JobStatus {
    CREATED("Created", "secondary", "bi-plus-circle"),
    ASSIGNED("Assigned", "info", "bi-person-check"),
    IN_PROGRESS("In Progress", "primary", "bi-play-circle"),
    COMPLETED("Completed", "warning", "bi-check2-circle"),
    APPROVED("Approved", "success", "bi-patch-check"),
    CANCELLED("Cancelled", "danger", "bi-x-circle");

    private final String label;
    private final String badgeColor;
    private final String icon;

    JobStatus(String label, String badgeColor, String icon) {
        this.label = label;
        this.badgeColor = badgeColor;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Legal next transitions for a job.
     */
    public Set<JobStatus> allowedNext() {
        switch (this) {
            case CREATED:
                return Set.of(ASSIGNED, IN_PROGRESS, CANCELLED);
            case ASSIGNED:
                return Set.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS:
                return Set.of(COMPLETED, CANCELLED);
            case COMPLETED:
                return Set.of(APPROVED, IN_PROGRESS, CANCELLED);
            case APPROVED:
            case CANCELLED:
            default:
                return Set.of();
        }
    }

    public boolean canTransitionTo(JobStatus target) {
        return target != null && allowedNext().contains(target);
    }

    public static JobStatus from(String value) {
        if (value == null) return CREATED;
        for (JobStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return CREATED;
    }
}

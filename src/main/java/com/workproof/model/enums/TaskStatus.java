package com.workproof.model.enums;

import java.util.Set;

/**
 * Task lifecycle:
 *   TODO -> IN_PROGRESS -> SUBMITTED -> IN_REVIEW -> APPROVED -> COMPLETED
 *                                      \\-> REJECTED -> (resubmit) -> SUBMITTED
 *
 * This enum is the single source of truth for which transitions are legal,
 * so the rule is enforced in ONE place (used by TaskService and unit tested).
 */
public enum TaskStatus {
    TODO("To Do", "secondary", "bi-circle"),
    IN_PROGRESS("In Progress", "primary", "bi-play-circle"),
    SUBMITTED("Submitted", "info", "bi-upload"),
    IN_REVIEW("In Review", "warning", "bi-hourglass-split"),
    APPROVED("Approved", "success", "bi-patch-check"),
    REJECTED("Rejected", "danger", "bi-x-octagon"),
    COMPLETED("Completed", "success", "bi-check2-circle");

    private final String label;
    private final String badgeColor;   // Bootstrap colour name
    private final String icon;         // Bootstrap icon class

    TaskStatus(String label, String badgeColor, String icon) {
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

    /** Legal next states for a task. */
    public Set<TaskStatus> allowedNext() {
        switch (this) {
            case TODO:        return Set.of(IN_PROGRESS, SUBMITTED);
            case IN_PROGRESS: return Set.of(TODO, SUBMITTED, IN_REVIEW);
            case SUBMITTED:   return Set.of(IN_REVIEW, IN_PROGRESS, APPROVED, REJECTED);
            case IN_REVIEW:   return Set.of(APPROVED, REJECTED, SUBMITTED);
            case APPROVED:    return Set.of(COMPLETED, IN_PROGRESS);
            case REJECTED:    return Set.of(IN_PROGRESS, SUBMITTED);
            case COMPLETED:   return Set.of(IN_PROGRESS);   // reopen
            default:          return Set.of();
        }
    }

    public boolean canMoveTo(TaskStatus target) {
        return target != null && allowedNext().contains(target);
    }

    /** true when the task is finished from a reporting point of view. */
    public boolean isDone() {
        return this == COMPLETED;
    }

    /** true while evidence is waiting for (or under) review. */
    public boolean isAwaitingVerification() {
        return this == SUBMITTED || this == IN_REVIEW;
    }

    public static TaskStatus from(String value) {
        if (value == null) {
            return TODO;
        }
        for (TaskStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return TODO;
    }
}

package com.workproof.model.enums;

/** Task priority (tasks.priority). */
public enum TaskPriority {
    LOW("Low", "success"),
    MEDIUM("Medium", "info"),
    HIGH("High", "warning"),
    CRITICAL("Critical", "danger");

    private final String label;
    private final String badgeColor;

    TaskPriority(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static TaskPriority from(String value) {
        if (value == null) {
            return MEDIUM;
        }
        for (TaskPriority p : values()) {
            if (p.name().equalsIgnoreCase(value.trim())) {
                return p;
            }
        }
        return MEDIUM;
    }
}

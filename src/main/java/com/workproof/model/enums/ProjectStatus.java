package com.workproof.model.enums;

/** Project lifecycle (projects.status). */
public enum ProjectStatus {
    PLANNED("Planned", "secondary"),
    ACTIVE("Active", "primary"),
    ON_HOLD("On Hold", "warning"),
    COMPLETED("Completed", "success"),
    ARCHIVED("Archived", "dark");

    private final String label;
    private final String badgeColor;

    ProjectStatus(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static ProjectStatus from(String value) {
        if (value == null) {
            return PLANNED;
        }
        for (ProjectStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return PLANNED;
    }
}

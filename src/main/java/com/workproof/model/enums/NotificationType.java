package com.workproof.model.enums;

/** Notification category (notifications.type). */
public enum NotificationType {
    TASK_ASSIGNED("bi-person-plus", "primary"),
    TASK_STATUS("bi-arrow-repeat", "info"),
    SUBMISSION("bi-cloud-upload", "secondary"),
    REVIEW("bi-patch-check", "success"),
    DEADLINE("bi-alarm", "warning"),
    PROJECT("bi-kanban", "dark"),
    SYSTEM("bi-gear", "secondary");

    private final String icon;
    private final String color;

    NotificationType(String icon, String color) {
        this.icon = icon;
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public static NotificationType from(String value) {
        if (value == null) {
            return SYSTEM;
        }
        for (NotificationType t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return SYSTEM;
    }
}

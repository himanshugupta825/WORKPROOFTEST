package com.workproof.model.enums;

/** Account state (users.status). Inactive accounts cannot log in. */
public enum UserStatus {
    ACTIVE("Active", "success"),
    INACTIVE("Inactive", "secondary");

    private final String label;
    private final String badgeColor;

    UserStatus(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static UserStatus from(String value) {
        if (value == null) {
            return ACTIVE;
        }
        for (UserStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return ACTIVE;
    }
}

package com.workproof.model.enums;

/**
 * Application-wide role. Stored in roles.name / users.role_id.
 * This is the GLOBAL role of an account (one per user).
 */
public enum RoleName {
    ADMIN("Administrator"),
    MANAGER("Project Manager"),
    MEMBER("Team Member"),
    REVIEWER("Reviewer / Mentor");

    private final String label;

    RoleName(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Safe lookup - returns MEMBER when the database value is unknown. */
    public static RoleName from(String value) {
        if (value == null) {
            return MEMBER;
        }
        for (RoleName r : values()) {
            if (r.name().equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        return MEMBER;
    }
}

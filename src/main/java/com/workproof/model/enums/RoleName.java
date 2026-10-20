package com.workproof.model.enums;

/**
 * Application-wide role for Spring Security and access control.
 * Supports ADMIN, MANAGER, TECHNICIAN, and CUSTOMER (with legacy aliases).
 */
public enum RoleName {
    ADMIN("Administrator"),
    MANAGER("Manager"),
    TECHNICIAN("Technician"),
    CUSTOMER("Customer"),
    MEMBER("Technician / Member"),
    REVIEWER("Reviewer / Mentor");

    private final String label;

    RoleName(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Safe lookup - defaults to MEMBER if unknown */
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

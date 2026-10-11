package com.workproof.model.enums;

/**
 * The role a user holds INSIDE a single project (project_members.role_in_project).
 * A global MEMBER can be a project MANAGER, and a global REVIEWER can review one project.
 */
public enum ProjectRole {
    MANAGER("Manager"),
    MEMBER("Member"),
    REVIEWER("Reviewer");

    private final String label;

    ProjectRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProjectRole from(String value) {
        if (value == null) {
            return MEMBER;
        }
        for (ProjectRole r : values()) {
            if (r.name().equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        return MEMBER;
    }
}

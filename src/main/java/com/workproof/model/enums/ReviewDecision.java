package com.workproof.model.enums;

/** Reviewer decision (reviews.decision). */
public enum ReviewDecision {
    APPROVED("Approved", "success"),
    REJECTED("Rejected", "danger");

    private final String label;
    private final String badgeColor;

    ReviewDecision(String label, String badgeColor) {
        this.label = label;
        this.badgeColor = badgeColor;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static ReviewDecision from(String value) {
        if (value == null) {
            return REJECTED;
        }
        for (ReviewDecision d : values()) {
            if (d.name().equalsIgnoreCase(value.trim())) {
                return d;
            }
        }
        return REJECTED;
    }
}

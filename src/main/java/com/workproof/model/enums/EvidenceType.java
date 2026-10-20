package com.workproof.model.enums;

public enum EvidenceType {
    BEFORE_PHOTO("Before Photo", "bi-camera"),
    AFTER_PHOTO("After Photo", "bi-camera-fill"),
    DOCUMENT("Document", "bi-file-earmark-text"),
    NOTE("Note", "bi-journal-text");

    private final String label;
    private final String icon;

    EvidenceType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}

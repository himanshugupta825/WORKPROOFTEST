package com.workproof.model;

import java.time.LocalDateTime;

/** Row of `activity_logs` - the audit trail shown on dashboards and the admin area. */
public class ActivityLog {

    private int id;
    private Integer userId;      // null for system events
    private String action;
    private String entityType;
    private Integer entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    /* joined */
    private String userName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Integer getEntityId() { return entityId; }
    public void setEntityId(Integer entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    /** "PROJECT_CREATED" -> "Project Created" for display. */
    public String getActionLabel() {
        if (action == null) {
            return "";
        }
        String[] words = action.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                sb.append(w.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    public String getActorName() {
        return userName == null ? "System" : userName;
    }

    /** Bootstrap icon representing the action family. */
    public String getIconClass() {
        if (action == null) return "bi-activity";
        if (action.startsWith("PROJECT")) return "bi-kanban";
        if (action.startsWith("TASK")) return "bi-list-check";
        if (action.startsWith("SUBMISSION")) return "bi-cloud-upload";
        if (action.startsWith("REVIEW")) return "bi-patch-check";
        if (action.startsWith("USER") || action.startsWith("AUTH")) return "bi-person-gear";
        return "bi-activity";
    }
}

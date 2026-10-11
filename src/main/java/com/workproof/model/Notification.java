package com.workproof.model;

import com.workproof.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Row of `notifications`. */
public class Notification {

    private int id;
    private int userId;
    private String title;
    private String message;
    private String type;
    private String link;
    private String dedupeKey;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public NotificationType getNotificationType() {
        return NotificationType.from(type);
    }

    /** "5 minutes ago" style label. */
    public String getRelativeTime() {
        if (createdAt == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createdAt, now);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = ChronoUnit.HOURS.between(createdAt, now);
        if (hours < 24) return hours + " h ago";
        long days = ChronoUnit.DAYS.between(createdAt, now);
        if (days == 1) return "yesterday";
        if (days < 30) return days + " days ago";
        return createdAt.toLocalDate().toString();
    }
}

package com.workproof.dao;

import com.workproof.model.Notification;
import com.workproof.model.PagedResult;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Data access for `notifications`. */
public class NotificationDao extends BaseDao {

    private static final String BASE_SELECT =
            "SELECT id, user_id, title, message, type, link, dedupe_key, is_read, created_at, read_at "
          + "FROM notifications ";

    private final RowMapper<Notification> mapper = (ResultSet rs) -> {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setLink(rs.getString("link"));
        n.setDedupeKey(rs.getString("dedupe_key"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        n.setReadAt(toLocalDateTime(rs.getTimestamp("read_at")));
        return n;
    };

    /* ================================ reads ================================ */

    public List<Notification> findRecentForUser(int userId, int limit) {
        return list(BASE_SELECT + "WHERE user_id = ? ORDER BY created_at DESC LIMIT ?", mapper, userId, limit);
    }

    public PagedResult<Notification> searchForUser(int userId, Boolean unreadOnly, String type,
                                                   int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (Boolean.TRUE.equals(unreadOnly)) {
            where.append(" AND is_read = 0 ");
        }
        if (type != null && !type.isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            where.append(" AND type = ? ");
            params.add(type);
        }

        int total = count("SELECT COUNT(*) FROM notifications " + where, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pageSize);
        pageParams.add((Math.max(page, 1) - 1) * pageSize);

        List<Notification> items = list(BASE_SELECT + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                mapper, pageParams.toArray());
        return new PagedResult<>(items, total, page, pageSize);
    }

    public int countUnread(int userId) {
        return count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0", userId);
    }

    public int countAll(int userId) {
        return count("SELECT COUNT(*) FROM notifications WHERE user_id = ?", userId);
    }

    /* ================================ writes ================================ */

    /**
     * Creates a notification. The (user_id, dedupe_key) unique constraint makes this
     * idempotent for scheduled jobs: the INSERT is ignored when the same event was
     * already delivered.
     */
    public int insert(int userId, String title, String message, String type, String link, String dedupeKey) {
        return update("INSERT IGNORE INTO notifications (user_id, title, message, type, link, dedupe_key) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                userId, title, message, type, link, dedupeKey);
    }

    public int markAsRead(int notificationId, int userId) {
        return update("UPDATE notifications SET is_read = 1, read_at = NOW() WHERE id = ? AND user_id = ?",
                notificationId, userId);
    }

    public int markAllAsRead(int userId) {
        return update("UPDATE notifications SET is_read = 1, read_at = NOW() WHERE user_id = ? AND is_read = 0",
                userId);
    }

    public int delete(int notificationId, int userId) {
        return update("DELETE FROM notifications WHERE id = ? AND user_id = ?", notificationId, userId);
    }

    /** Housekeeping: keeps the table small during a long demo. */
    public int deleteOlderThan(int days) {
        return update("DELETE FROM notifications WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)", days);
    }
}

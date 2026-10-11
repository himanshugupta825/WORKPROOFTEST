package com.workproof.service;

import com.workproof.dao.ActivityLogDao;
import com.workproof.model.ActivityLog;
import com.workproof.model.PagedResult;
import com.workproof.model.User;

import java.time.LocalDate;
import java.util.List;

/**
 * Writes and reads the activity log (audit trail).
 *
 * Every important action in WorkProof goes through one of the {@code record*}
 * methods, which is why the reports and the admin activity page can always show
 * who did what and when.
 *
 * Action names are constants so typos cannot silently break the log.
 */
public class ActivityService {

    /* ---- action names ---- */
    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String AUTH_REGISTER = "AUTH_REGISTER";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PROFILE_UPDATED = "PROFILE_UPDATED";

    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_UPDATED = "PROJECT_UPDATED";
    public static final String PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String PROJECT_RESTORED = "PROJECT_RESTORED";
    public static final String PROJECT_MEMBER_ADDED = "PROJECT_MEMBER_ADDED";
    public static final String PROJECT_MEMBER_REMOVED = "PROJECT_MEMBER_REMOVED";
    public static final String PROJECT_MEMBER_ROLE_CHANGED = "PROJECT_MEMBER_ROLE_CHANGED";

    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_UPDATED = "TASK_UPDATED";
    public static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_DELETED = "TASK_DELETED";

    public static final String SUBMISSION_CREATED = "SUBMISSION_CREATED";
    public static final String SUBMISSION_FILE_DOWNLOADED = "SUBMISSION_FILE_DOWNLOADED";

    public static final String REVIEW_APPROVED = "REVIEW_APPROVED";
    public static final String REVIEW_REJECTED = "REVIEW_REJECTED";

    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    /* =============================== reads =============================== */

    public List<ActivityLog> recent(int limit) {
        return activityLogDao.findRecent(limit);
    }

    public List<ActivityLog> recentForUser(int userId, int limit) {
        return activityLogDao.findRecentForUser(userId, limit);
    }

    public List<ActivityLog> recentForProject(int projectId, int limit) {
        return activityLogDao.findRecentForProject(projectId, limit);
    }

    public List<ActivityLog> recentForEntity(String entityType, int entityId, int limit) {
        return activityLogDao.findRecentForEntity(entityType, entityId, limit);
    }

    public PagedResult<ActivityLog> search(String search, String action, Integer userId,
                                           LocalDate from, LocalDate to, int page, int pageSize) {
        return activityLogDao.search(search, action, userId, from, to, page, pageSize);
    }

    /* =============================== write =============================== */

    /** Core logging method - the entity type is stored so logs can be filtered later. */
    public void log(User actor, String action, String entityType, Integer entityId, String details, String ip) {
        activityLogDao.log(actor == null ? null : actor.getId(), action, entityType, entityId, details, ip);
    }

    /**
     * Log an event that has no User object (for example a scheduled job or a failed
     * login where the account could not be resolved). Passing null stores a system event.
     */
    public void logForUserId(Integer userId, String action, String entityType, Integer entityId,
                             String details, String ip) {
        activityLogDao.log(userId, action, entityType, entityId, details, ip);
    }
}

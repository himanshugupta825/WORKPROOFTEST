package com.workproof.service;

import com.workproof.dao.NotificationDao;
import com.workproof.model.Notification;
import com.workproof.model.PagedResult;
import com.workproof.model.ProofSubmission;
import com.workproof.model.Task;
import com.workproof.model.User;
import com.workproof.model.enums.NotificationType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates in-app notifications for the events listed in the specification:
 * task assignment, review decisions, approaching deadlines and status changes.
 *
 * Design notes:
 *   - Every notification carries a deep link so the user can jump straight to the page.
 *   - Scheduled deadline warnings use a dedupe key, so the same task never produces
 *     two identical reminders.
 *   - Notification failures never break the main business operation.
 */
public class NotificationService {

    private final NotificationDao notificationDao = new NotificationDao();

    /* ============================== user facing ============================== */

    public List<Notification> recentForUser(int userId, int limit) {
        return notificationDao.findRecentForUser(userId, limit);
    }

    public PagedResult<Notification> search(int userId, Boolean unreadOnly, String type, int page, int pageSize) {
        return notificationDao.searchForUser(userId, unreadOnly, type, page, pageSize);
    }

    public int unreadCount(int userId) {
        return notificationDao.countUnread(userId);
    }

    public int totalCount(int userId) {
        return notificationDao.countAll(userId);
    }

    public void markAsRead(int notificationId, int userId) {
        notificationDao.markAsRead(notificationId, userId);
    }

    public void markAllAsRead(int userId) {
        notificationDao.markAllAsRead(userId);
    }

    public void delete(int notificationId, int userId) {
        notificationDao.delete(notificationId, userId);
    }

    /* ============================ event helpers ============================ */

    public void notifyTaskAssigned(Task task, Collection<Integer> assigneeIds, User actor) {
        if (task == null || assigneeIds == null) {
            return;
        }
        for (Integer userId : new LinkedHashSet<>(assigneeIds)) {
            if (userId == null || (actor != null && userId == actor.getId())) {
                continue;   // no self-notification
            }
            send(userId, "New task assigned",
                    actorName(actor) + " assigned you the task '" + task.getTitle()
                            + "' in " + task.getProjectName() + " (due " + task.getDueDate() + ").",
                    NotificationType.TASK_ASSIGNED, "/tasks/view?id=" + task.getId(), null);
        }
    }

    public void notifyTaskStatusChanged(Task task, User actor, String oldStatus, String newStatus) {
        if (task == null) {
            return;
        }
        String message = actorName(actor) + " moved '" + task.getTitle() + "' from "
                + oldStatus + " to " + newStatus + ".";
        Set<Integer> recipients = new LinkedHashSet<>();
        for (var assignee : task.getAssignees()) {
            recipients.add(assignee.getUserId());
        }
        for (Integer userId : recipients) {
            if (userId == null || (actor != null && userId == actor.getId())) {
                continue;
            }
            send(userId, "Task status updated", message, NotificationType.TASK_STATUS,
                    "/tasks/view?id=" + task.getId(), null);
        }
    }

    /** Notifies managers and reviewers of a project that new evidence is waiting. */
    public void notifySubmissionReceived(ProofSubmission submission, Task task, List<Integer> reviewerIds,
                                         User actor) {
        for (Integer reviewerId : new LinkedHashSet<>(reviewerIds)) {
            if (reviewerId == null || (actor != null && reviewerId == actor.getId())) {
                continue;
            }
            send(reviewerId, "Evidence awaiting verification",
                    actorName(actor) + " submitted proof for '" + task.getTitle() + "' (version "
                            + submission.getVersion() + "). Please review it.",
                    NotificationType.SUBMISSION, "/reviews/view?id=" + submission.getId(), null);
        }
    }

    public void notifyReviewDecision(ProofSubmission submission, Task task, User reviewer,
                                     String decision, String comments) {
        if (submission == null) {
            return;
        }
        boolean approved = "APPROVED".equalsIgnoreCase(decision);
        StringBuilder message = new StringBuilder();
        message.append(actorName(reviewer)).append(approved ? " approved " : " rejected ")
               .append("your evidence for '").append(task == null ? "the task" : task.getTitle())
               .append("'.");
        if (comments != null && !comments.isBlank()) {
            message.append(" Feedback: ").append(trim(comments, 200));
        }
        if (!approved) {
            message.append(" You may correct the work and submit a new version.");
        }
        send(submission.getSubmittedBy(), approved ? "Submission approved" : "Submission rejected",
                message.toString(), NotificationType.REVIEW,
                "/submissions/view?id=" + submission.getId(), null);
    }

    public void notifyDeadlineApproaching(int userId, Task task, long daysLeft) {
        String when = daysLeft < 0
                ? Math.abs(daysLeft) + " day(s) overdue"
                : (daysLeft == 0 ? "due today" : "due in " + daysLeft + " day(s)");
        String dedupe = "deadline:" + task.getId() + ":" + java.time.LocalDate.now() + ":" + daysLeft;
        send(userId, "Deadline reminder",
                "Task '" + task.getTitle() + "' is " + when + " (" + task.getDueDate() + ").",
                NotificationType.DEADLINE, "/tasks/view?id=" + task.getId(), dedupe);
    }

    /** Tells a user they were added to a project, including the role they received. */
    public void notifyProjectMembership(int userId, int projectId, String projectName,
                                        String projectRole, User actor) {
        send(userId, "Added to project",
                actorName(actor) + " added you to '" + projectName + "' as " + projectRole + ".",
                NotificationType.PROJECT, "/projects/view?id=" + projectId, null);
    }

    /** Generic send used by controllers for one-off messages. */
    public void send(int userId, String title, String message, NotificationType type, String link,
                     String dedupeKey) {
        try {
            notificationDao.insert(userId, trim(title, 150), trim(message, 500), type.name(), link, dedupeKey);
        } catch (RuntimeException e) {
            // A failed notification must never roll back the business action.
            System.err.println("[WorkProof] Notification not created for user " + userId + ": " + e.getMessage());
        }
    }

    private static String actorName(User actor) {
        return actor == null ? "The system" : actor.getFullName();
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String single = value.replace('\n', ' ').replace('\r', ' ').trim();
        return single.length() <= max ? single : single.substring(0, max - 3) + "...";
    }
}

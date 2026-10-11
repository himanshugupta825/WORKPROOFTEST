package com.workproof.model;

import com.workproof.model.enums.NotificationType;
import com.workproof.model.enums.ProjectRole;
import com.workproof.model.enums.ProjectStatus;
import com.workproof.model.enums.ReviewDecision;
import com.workproof.model.enums.RoleName;
import com.workproof.model.enums.SubmissionStatus;
import com.workproof.model.enums.TaskPriority;
import com.workproof.model.enums.UserStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Values coming out of the database, a form field or a query string are read with
 * the lenient {@code from(...)} helper of each enum. A typo must never throw a
 * NullPointerException in front of the examiner: every enum has a documented
 * fallback, and the database column values are exactly the enum names.
 */
class EnumParsingTest {

    @Test
    @DisplayName("task priority: unknown input falls back to MEDIUM")
    void taskPriority() {
        assertEquals(TaskPriority.CRITICAL, TaskPriority.from("critical"));
        assertEquals(TaskPriority.HIGH, TaskPriority.from("HIGH"));
        assertEquals(TaskPriority.MEDIUM, TaskPriority.from(null));
        assertEquals(TaskPriority.MEDIUM, TaskPriority.from("urgent"));
    }

    @Test
    @DisplayName("project status: unknown input falls back to PLANNED")
    void projectStatus() {
        assertEquals(ProjectStatus.ACTIVE, ProjectStatus.from("active"));
        assertEquals(ProjectStatus.ARCHIVED, ProjectStatus.from("ARCHIVED"));
        assertEquals(ProjectStatus.PLANNED, ProjectStatus.from(null));
        assertEquals(ProjectStatus.PLANNED, ProjectStatus.from("cancelled"));
    }

    @Test
    @DisplayName("submission status: unknown input falls back to PENDING, never to APPROVED")
    void submissionStatus() {
        assertEquals(SubmissionStatus.APPROVED, SubmissionStatus.from("approved"));
        assertEquals(SubmissionStatus.SUPERSEDED, SubmissionStatus.from("superseded"));
        assertEquals(SubmissionStatus.PENDING, SubmissionStatus.from(null));
        assertEquals(SubmissionStatus.PENDING, SubmissionStatus.from("garbage"),
                "a broken value must fail safe: it must NOT read as APPROVED");
    }

    @Test
    @DisplayName("review decision: unknown input falls back to the cautious value")
    void reviewDecision() {
        assertEquals(ReviewDecision.APPROVED, ReviewDecision.from("approved"));
        assertEquals(ReviewDecision.REJECTED, ReviewDecision.from(null));
        assertEquals(ReviewDecision.REJECTED, ReviewDecision.from("maybe"));
    }

    @Test
    @DisplayName("user status: unknown input never produces a privileged state")
    void userStatus() {
        assertEquals(UserStatus.INACTIVE, UserStatus.from("inactive"));
        assertEquals(UserStatus.ACTIVE, UserStatus.from(null));
        assertEquals(UserStatus.ACTIVE, UserStatus.from("zzz"));
    }

    @Test
    @DisplayName("notification type: unknown input falls back to SYSTEM")
    void notificationType() {
        assertEquals(NotificationType.SYSTEM, NotificationType.from(null));
        assertEquals(NotificationType.SYSTEM, NotificationType.from("nonsense"));
        assertNotNull(NotificationType.from("TASK_ASSIGNED"));
    }

    @Test
    @DisplayName("global and in-project roles are parsed case-insensitively")
    void roles() {
        assertEquals(RoleName.ADMIN, RoleName.from("admin"));
        assertEquals(RoleName.MEMBER, RoleName.from(null), "the least privileged role is the fallback");
        assertEquals(ProjectRole.REVIEWER, ProjectRole.from("REVIEWER"));
        assertEquals(ProjectRole.MEMBER, ProjectRole.from(null));
    }

    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"MANAGER", "MEMBER", "REVIEWER"})
    @DisplayName("managers, members and reviewers may self-register; admins may not")
    void selfRegisterableRoles(RoleName role) {
        assertTrue(com.workproof.service.Permissions.isSelfRegisterableRole(role));
    }

    @Test
    @DisplayName("administrators cannot be created through public registration")
    void adminCannotSelfRegister() {
        assertFalse(com.workproof.service.Permissions.isSelfRegisterableRole(RoleName.ADMIN));
    }

    @Test
    @DisplayName("the database column values are the enum names (no display labels stored)")
    void namesAreTheStorageFormat() {
        assertEquals("HIGH", TaskPriority.HIGH.name());
        assertEquals("ON_HOLD", ProjectStatus.ON_HOLD.name());
        assertEquals("TASK_ASSIGNED", NotificationType.TASK_ASSIGNED.name());
        assertEquals("ADMIN", RoleName.ADMIN.name());
    }
}

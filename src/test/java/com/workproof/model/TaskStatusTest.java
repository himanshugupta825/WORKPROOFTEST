package com.workproof.model;

import com.workproof.model.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The task workflow is the heart of the system, so its rules live in one enum and
 * are tested as a state machine instead of being scattered through the UI.
 *
 * TODO -> IN_PROGRESS -> SUBMITTED -> IN_REVIEW -> APPROVED -> COMPLETED
 *                                 \-> REJECTED -> (resubmit) -> SUBMITTED
 */
class TaskStatusTest {

    @Test
    @DisplayName("the happy path is legal at every step")
    void happyPath() {
        assertTrue(TaskStatus.TODO.canMoveTo(TaskStatus.IN_PROGRESS));
        assertTrue(TaskStatus.IN_PROGRESS.canMoveTo(TaskStatus.SUBMITTED));
        assertTrue(TaskStatus.SUBMITTED.canMoveTo(TaskStatus.IN_REVIEW));
        assertTrue(TaskStatus.IN_REVIEW.canMoveTo(TaskStatus.APPROVED));
        assertTrue(TaskStatus.APPROVED.canMoveTo(TaskStatus.COMPLETED));
    }

    @Test
    @DisplayName("a submitted task can be approved or rejected directly from the queue")
    void reviewerCanDecideFromSubmitted() {
        assertTrue(TaskStatus.SUBMITTED.canMoveTo(TaskStatus.APPROVED));
        assertTrue(TaskStatus.SUBMITTED.canMoveTo(TaskStatus.REJECTED));
    }

    @Test
    @DisplayName("a rejected task can be worked on again or resubmitted immediately")
    void rejectionIsRecoverable() {
        assertTrue(TaskStatus.REJECTED.canMoveTo(TaskStatus.IN_PROGRESS));
        assertTrue(TaskStatus.REJECTED.canMoveTo(TaskStatus.SUBMITTED));
    }

    @Test
    @DisplayName("a completed task can be reopened, which is how corrections are handled")
    void completedCanBeReopened() {
        assertTrue(TaskStatus.COMPLETED.canMoveTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("illegal jumps are refused - no skipping the workflow")
    void illegalJumps() {
        assertFalse(TaskStatus.TODO.canMoveTo(TaskStatus.APPROVED),
                "work must be submitted before it can be approved");
        assertFalse(TaskStatus.TODO.canMoveTo(TaskStatus.COMPLETED));
        assertFalse(TaskStatus.TODO.canMoveTo(TaskStatus.IN_REVIEW));
        assertFalse(TaskStatus.IN_PROGRESS.canMoveTo(TaskStatus.APPROVED),
                "a reviewer decides on evidence, not on a task that was never submitted");
        assertFalse(TaskStatus.IN_PROGRESS.canMoveTo(TaskStatus.COMPLETED));
        assertFalse(TaskStatus.IN_REVIEW.canMoveTo(TaskStatus.COMPLETED),
                "IN_REVIEW must first become APPROVED");
        assertFalse(TaskStatus.APPROVED.canMoveTo(TaskStatus.SUBMITTED),
                "an approved task does not go back to waiting for review");
        assertFalse(TaskStatus.COMPLETED.canMoveTo(TaskStatus.COMPLETED));
    }

    @Test
    @DisplayName("a task never transitions to itself, and null is refused")
    void selfAndNullTransitions() {
        for (TaskStatus status : TaskStatus.values()) {
            assertFalse(status.canMoveTo(status), status + " must not transition to itself");
        }
        assertFalse(TaskStatus.TODO.canMoveTo(null));
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    @DisplayName("every status has at least one legal next step (no dead ends)")
    void noDeadEnds(TaskStatus status) {
        assertFalse(status.allowedNext().isEmpty(),
                status + " has no legal transition, which would freeze the task forever");
    }

    @Test
    @DisplayName("isDone and isAwaitingVerification drive the business rules")
    void helperFlags() {
        assertTrue(TaskStatus.COMPLETED.isDone());

        assertTrue(TaskStatus.SUBMITTED.isAwaitingVerification());
        assertTrue(TaskStatus.IN_REVIEW.isAwaitingVerification());
        assertFalse(TaskStatus.IN_PROGRESS.isAwaitingVerification());
        assertFalse(TaskStatus.APPROVED.isAwaitingVerification());
        assertFalse(TaskStatus.TODO.isDone());
    }

    @Test
    @DisplayName("reading a status from the database or a form is forgiving, never crashing")
    void parsingIsLenient() {
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.from("in_progress"));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.from("  IN_PROGRESS  "));
        assertEquals(TaskStatus.COMPLETED, TaskStatus.from("Completed"));
        assertEquals(TaskStatus.TODO, TaskStatus.from(null), "unknown values fall back to TODO");
        assertEquals(TaskStatus.TODO, TaskStatus.from("NOT_A_STATUS"));
    }

    @Test
    @DisplayName("each status has a label, a badge colour and an icon for the UI")
    void presentationMetadata() {
        for (TaskStatus status : TaskStatus.values()) {
            assertFalse(status.getLabel().isBlank(), status + " needs a label");
            assertFalse(status.getBadgeColor().isBlank(), status + " needs a badge colour");
            assertTrue(status.getIcon().startsWith("bi-"), status + " needs a bootstrap icon class");
        }
    }
}

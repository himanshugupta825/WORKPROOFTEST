package com.workproof.model;

import com.workproof.model.enums.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobStatusTransitionTest {

    @Test
    @DisplayName("Status flow CREATED -> ASSIGNED -> IN_PROGRESS -> COMPLETED -> APPROVED is valid")
    void validLifecycleTransitions() {
        assertTrue(JobStatus.CREATED.canTransitionTo(JobStatus.ASSIGNED));
        assertTrue(JobStatus.ASSIGNED.canTransitionTo(JobStatus.IN_PROGRESS));
        assertTrue(JobStatus.IN_PROGRESS.canTransitionTo(JobStatus.COMPLETED));
        assertTrue(JobStatus.COMPLETED.canTransitionTo(JobStatus.APPROVED));
    }

    @Test
    @DisplayName("Direct start from CREATED to IN_PROGRESS is allowed for walk-in/self-assigned jobs")
    void createdToInProgressAllowed() {
        assertTrue(JobStatus.CREATED.canTransitionTo(JobStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("Cancellation is allowed from CREATED, ASSIGNED, IN_PROGRESS, and COMPLETED")
    void cancellationAllowedFromNonTerminalStates() {
        assertTrue(JobStatus.CREATED.canTransitionTo(JobStatus.CANCELLED));
        assertTrue(JobStatus.ASSIGNED.canTransitionTo(JobStatus.CANCELLED));
        assertTrue(JobStatus.IN_PROGRESS.canTransitionTo(JobStatus.CANCELLED));
        assertTrue(JobStatus.COMPLETED.canTransitionTo(JobStatus.CANCELLED));
    }

    @Test
    @DisplayName("Invalid lifecycle transitions are strictly rejected")
    void invalidTransitionsRejected() {
        assertFalse(JobStatus.CREATED.canTransitionTo(JobStatus.APPROVED), "Cannot approve an uncompleted job");
        assertFalse(JobStatus.ASSIGNED.canTransitionTo(JobStatus.APPROVED), "Cannot jump from ASSIGNED directly to APPROVED");
        assertFalse(JobStatus.APPROVED.canTransitionTo(JobStatus.IN_PROGRESS), "APPROVED is a terminal state");
        assertFalse(JobStatus.APPROVED.canTransitionTo(JobStatus.CANCELLED), "Cannot cancel an already approved job");
        assertFalse(JobStatus.CANCELLED.canTransitionTo(JobStatus.IN_PROGRESS), "CANCELLED is a terminal state");
        assertFalse(JobStatus.COMPLETED.canTransitionTo(JobStatus.CREATED), "Cannot revert COMPLETED back to CREATED");
    }
}


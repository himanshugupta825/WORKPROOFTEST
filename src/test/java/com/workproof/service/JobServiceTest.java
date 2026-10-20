package com.workproof.service;

import com.workproof.exception.AuthorizationException;
import com.workproof.exception.ValidationException;
import com.workproof.model.*;
import com.workproof.model.enums.JobStatus;
import com.workproof.model.enums.RoleName;
import com.workproof.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobServiceTest {

    private JobRepository jobRepository;
    private ChecklistItemRepository checklistItemRepository;
    private ApprovalRepository approvalRepository;
    private AuditLogRepository auditLogRepository;
    private AuditService auditService;
    private CustomerRepository customerRepository;

    private JobService jobService;

    private User technician;
    private User customerUser;
    private User manager;
    private Customer customer;
    private Location location;
    private Job job;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        checklistItemRepository = mock(ChecklistItemRepository.class);
        approvalRepository = mock(ApprovalRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        auditService = new AuditService(auditLogRepository);
        customerRepository = mock(CustomerRepository.class);

        jobService = new JobService(jobRepository, checklistItemRepository, approvalRepository, auditService, customerRepository);

        technician = new User("Alex Rivers", "tech@workproof.com", "hash", RoleName.TECHNICIAN);
        technician.setId(10);

        customerUser = new User("Jane Customer", "customer@workproof.com", "hash", RoleName.CUSTOMER);
        customerUser.setId(20);

        manager = new User("Boss Manager", "manager@workproof.com", "hash", RoleName.MANAGER);
        manager.setId(30);

        customer = new Customer("Acme Inc", "Jane Customer", "customer@workproof.com", "555-0100", "123 Main St");
        customer.setId(1L);
        customer.setUser(customerUser);

        location = new Location("HQ Building", "100 Market St", "San Francisco", "94105", "Guard", "555-0199");
        location.setId(1L);

        job = new Job("Filter Replacement", "Replace HVAC filters", customer, location, manager);
        job.setId(100L);
        job.setStatus(JobStatus.ASSIGNED);
        job.setAssignedTechnician(technician);

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(jobRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Starting a job records startedAt timestamp and transitions status to IN_PROGRESS")
    void startJobRecordsTimestampAndStatus() {
        Job started = jobService.startJob(100L, technician, "127.0.0.1");

        assertEquals(JobStatus.IN_PROGRESS, started.getStatus());
        assertNotNull(started.getStartedAt());
        verify(jobRepository).save(started);
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Completing a job is blocked when mandatory checklist items are not completed")
    void completeJobBlockedByPendingChecklist() {
        job.setStatus(JobStatus.IN_PROGRESS);
        when(checklistItemRepository.countByJobAndMandatoryTrueAndCompletedFalse(job)).thenReturn(2L);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                jobService.completeJob(100L, technician, "127.0.0.1")
        );

        assertTrue(ex.getMessage().contains("mandatory checklist item(s) are still pending"));
        assertNull(job.getCompletedAt());
        assertNull(job.getProofCode());
    }

    @Test
    @DisplayName("Completing a job generates an unguessable proof code and sets completedAt")
    void completeJobGeneratesProofCodeWhenChecklistSatisfied() {
        job.setStatus(JobStatus.IN_PROGRESS);
        when(checklistItemRepository.countByJobAndMandatoryTrueAndCompletedFalse(job)).thenReturn(0L);

        Job completed = jobService.completeJob(100L, technician, "127.0.0.1");

        assertEquals(JobStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertNotNull(completed.getProofCode());
        assertTrue(completed.getProofCode().length() >= 10, "Proof code should be an unguessable token");

        verify(auditLogRepository, atLeast(2)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Customer can review and approve a completed job")
    void customerApproveJob() {
        job.setStatus(JobStatus.COMPLETED);
        job.setProofCode("PROOF12345");

        Job approved = jobService.approveJob(100L, "Jane Customer", "Work looks great!", customerUser, "127.0.0.1");

        assertEquals(JobStatus.APPROVED, approved.getStatus());
        assertNotNull(approved.getApproval());
        assertEquals("Jane Customer", approved.getApproval().getApproverName());
        assertEquals("Work looks great!", approved.getApproval().getComments());

        verify(approvalRepository).save(any(Approval.class));
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }
}


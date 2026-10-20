package com.workproof.service;

import com.workproof.exception.AuthorizationException;
import com.workproof.model.Customer;
import com.workproof.model.Job;
import com.workproof.model.Location;
import com.workproof.model.User;
import com.workproof.model.enums.JobStatus;
import com.workproof.model.enums.RoleName;
import com.workproof.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SecurityAccessControlTest {

    private JobRepository jobRepository;
    private ChecklistItemRepository checklistItemRepository;
    private ApprovalRepository approvalRepository;
    private AuditService auditService;
    private CustomerRepository customerRepository;

    private JobService jobService;

    private User technicianAssigned;
    private User technicianUnassigned;
    private User customerUserOwner;
    private User customerUserOther;
    private User adminUser;

    private Job job;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        checklistItemRepository = mock(ChecklistItemRepository.class);
        approvalRepository = mock(ApprovalRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        auditService = new AuditService(auditLogRepository);
        customerRepository = mock(CustomerRepository.class);

        jobService = new JobService(jobRepository, checklistItemRepository, approvalRepository, auditService, customerRepository);

        technicianAssigned = new User("Alex Assigned", "tech1@workproof.com", "hash", RoleName.TECHNICIAN);
        technicianAssigned.setId(101);

        technicianUnassigned = new User("Sam Intruder", "tech2@workproof.com", "hash", RoleName.TECHNICIAN);
        technicianUnassigned.setId(102);

        customerUserOwner = new User("Acme Client", "client1@workproof.com", "hash", RoleName.CUSTOMER);
        customerUserOwner.setId(201);

        customerUserOther = new User("Other Client", "client2@workproof.com", "hash", RoleName.CUSTOMER);
        customerUserOther.setId(202);

        adminUser = new User("Super Admin", "admin@workproof.com", "hash", RoleName.ADMIN);
        adminUser.setId(301);

        Customer customer = new Customer("Acme Corp", "Acme Client", "client1@workproof.com", "555-0100", "123 Main St");
        customer.setId(1L);
        customer.setUser(customerUserOwner);

        Location location = new Location("HQ Building", "100 Market St", "San Francisco", "94105", "Guard", "555-0199");
        location.setId(1L);

        job = new Job("Electrical Inspection", "Inspect circuit breakers", customer, location, adminUser);
        job.setId(500L);
        job.setStatus(JobStatus.ASSIGNED);
        job.setAssignedTechnician(technicianAssigned);

        when(jobRepository.findById(500L)).thenReturn(Optional.of(job));
    }

    @Test
    @DisplayName("Assigned technician can act on their own job")
    void assignedTechnicianAllowed() {
        assertDoesNotThrow(() -> jobService.checkTechnicianOwnershipOrManager(job, technicianAssigned));
    }

    @Test
    @DisplayName("Unassigned technician is denied access from acting on another technician's job")
    void unassignedTechnicianDenied() {
        assertThrows(AuthorizationException.class, () ->
                jobService.checkTechnicianOwnershipOrManager(job, technicianUnassigned)
        );
    }

    @Test
    @DisplayName("Owner customer can approve their own job")
    void customerOwnerAllowed() {
        assertDoesNotThrow(() -> jobService.checkCustomerOwnershipOrManager(job, customerUserOwner));
    }

    @Test
    @DisplayName("Different customer cannot approve another customer's job")
    void customerOtherDenied() {
        assertThrows(AuthorizationException.class, () ->
                jobService.checkCustomerOwnershipOrManager(job, customerUserOther)
        );
    }

    @Test
    @DisplayName("Administrator and Manager have supervisory access to all jobs")
    void adminAllowed() {
        assertDoesNotThrow(() -> jobService.checkTechnicianOwnershipOrManager(job, adminUser));
        assertDoesNotThrow(() -> jobService.checkCustomerOwnershipOrManager(job, adminUser));
    }
}


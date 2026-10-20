package com.workproof.service;

import com.workproof.exception.AuthorizationException;
import com.workproof.exception.NotFoundException;
import com.workproof.exception.ValidationException;
import com.workproof.model.*;
import com.workproof.model.enums.AuditAction;
import com.workproof.model.enums.JobStatus;
import com.workproof.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;
    private final CustomerRepository customerRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    public JobService(JobRepository jobRepository,
                      ChecklistItemRepository checklistItemRepository,
                      ApprovalRepository approvalRepository,
                      AuditService auditService,
                      CustomerRepository customerRepository) {
        this.jobRepository = jobRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<Job> getJobsForUser(User user, JobStatus statusFilter) {
        if (user.isAdmin() || user.isManager()) {
            if (statusFilter != null) {
                return jobRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
            }
            return jobRepository.findAllByOrderByCreatedAtDesc();
        } else if (user.isTechnician()) {
            if (statusFilter != null) {
                return jobRepository.findByAssignedTechnicianAndStatusOrderByCreatedAtDesc(user, statusFilter);
            }
            return jobRepository.findByAssignedTechnicianOrderByCreatedAtDesc(user);
        } else if (user.isCustomer()) {
            Optional<Customer> custOpt = customerRepository.findByUser(user)
                    .or(() -> customerRepository.findByEmail(user.getEmail()));
            if (custOpt.isPresent()) {
                if (statusFilter != null) {
                    return jobRepository.findByCustomerAndStatusOrderByCreatedAtDesc(custOpt.get(), statusFilter);
                }
                return jobRepository.findByCustomerOrderByCreatedAtDesc(custOpt.get());
            }
            return List.of();
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public Job getJobById(Long id) {
        Job job = jobRepository.findByIdWithDetails(id)
                .or(() -> jobRepository.findById(id))
                .orElseThrow(() -> new NotFoundException("Job not found with id: " + id));
        if (job.getChecklistItems() != null) {
            job.getChecklistItems().size();
        }
        if (job.getEvidenceList() != null) {
            job.getEvidenceList().size();
        }
        return job;
    }

    @Transactional(readOnly = true)
    public Optional<Job> getJobByProofCode(String proofCode) {
        if (proofCode == null || proofCode.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Job> opt = jobRepository.findByProofCode(proofCode.trim());
        opt.ifPresent(job -> {
            if (job.getChecklistItems() != null) {
                job.getChecklistItems().size();
            }
            if (job.getEvidenceList() != null) {
                job.getEvidenceList().size();
            }
        });
        return opt;
    }

    @Transactional
    public Job createJob(String title, String description, Customer customer, Location location,
                         User assignedTechnician, User creator, List<String> checklistTitles, String ip) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Job title cannot be blank");
        }
        if (customer == null) {
            throw new ValidationException("Customer is required");
        }
        if (location == null) {
            throw new ValidationException("Location is required");
        }

        Job job = new Job();
        job.setTitle(title.trim());
        job.setDescription(description != null ? description.trim() : "");
        job.setCustomer(customer);
        job.setLocation(location);
        job.setCreatedBy(creator);
        job.setStatus(JobStatus.CREATED);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        if (assignedTechnician != null) {
            job.setAssignedTechnician(assignedTechnician);
            job.setStatus(JobStatus.ASSIGNED);
        }

        Job saved = jobRepository.save(job);

        // Add default or specified checklist items
        if (checklistTitles != null && !checklistTitles.isEmpty()) {
            for (String itemTitle : checklistTitles) {
                if (itemTitle != null && !itemTitle.trim().isEmpty()) {
                    ChecklistItem item = new ChecklistItem(saved, itemTitle.trim(), true);
                    checklistItemRepository.save(item);
                }
            }
        } else {
            // Default mandatory checklist items
            checklistItemRepository.save(new ChecklistItem(saved, "Perform initial site assessment", true));
            checklistItemRepository.save(new ChecklistItem(saved, "Complete core service work", true));
            checklistItemRepository.save(new ChecklistItem(saved, "Safety and cleanup inspection", true));
        }

        auditService.log(AuditAction.JOB_CREATED, saved.getId(), saved.getTitle(), creator,
                "Job created for customer '" + customer.getName() + "' at location '" + location.getName() + "'", ip);

        if (assignedTechnician != null) {
            auditService.log(AuditAction.JOB_ASSIGNED, saved.getId(), saved.getTitle(), creator,
                    "Assigned technician: " + assignedTechnician.getFullName(), ip);
        }

        return saved;
    }

    @Transactional
    public Job updateJob(Long jobId, String title, String description, Location location, User actor, String ip) {
        Job job = getJobById(jobId);
        checkManagerOrAdmin(actor);

        if (title != null && !title.trim().isEmpty()) {
            job.setTitle(title.trim());
        }
        if (description != null) {
            job.setDescription(description.trim());
        }
        if (location != null) {
            job.setLocation(location);
        }
        job.setUpdatedAt(LocalDateTime.now());
        Job saved = jobRepository.save(job);

        auditService.log(AuditAction.JOB_UPDATED, saved.getId(), saved.getTitle(), actor, "Job details updated", ip);
        return saved;
    }

    @Transactional
    public Job assignTechnician(Long jobId, User technician, User actor, String ip) {
        Job job = getJobById(jobId);
        checkManagerOrAdmin(actor);

        if (technician == null) {
            throw new ValidationException("Technician must be selected");
        }

        job.setAssignedTechnician(technician);
        if (job.getStatus() == JobStatus.CREATED) {
            job.setStatus(JobStatus.ASSIGNED);
        }
        job.setUpdatedAt(LocalDateTime.now());
        Job saved = jobRepository.save(job);

        auditService.log(AuditAction.JOB_ASSIGNED, saved.getId(), saved.getTitle(), actor,
                "Assigned technician " + technician.getFullName() + " (" + technician.getEmail() + ")", ip);
        return saved;
    }

    @Transactional
    public Job startJob(Long jobId, User actor, String ip) {
        Job job = getJobById(jobId);
        checkTechnicianOwnershipOrManager(job, actor);

        if (!job.getStatus().canTransitionTo(JobStatus.IN_PROGRESS)) {
            throw new ValidationException("Cannot transition job from " + job.getStatus().getLabel() + " to In Progress");
        }

        job.setStatus(JobStatus.IN_PROGRESS);
        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        job.setUpdatedAt(LocalDateTime.now());
        Job saved = jobRepository.save(job);

        auditService.log(AuditAction.JOB_STARTED, saved.getId(), saved.getTitle(), actor, "Job started by technician", ip);
        return saved;
    }

    @Transactional
    public Job completeJob(Long jobId, User actor, String ip) {
        Job job = getJobById(jobId);
        checkTechnicianOwnershipOrManager(job, actor);

        if (!job.getStatus().canTransitionTo(JobStatus.COMPLETED)) {
            throw new ValidationException("Cannot transition job from " + job.getStatus().getLabel() + " to Completed");
        }

        // Checklist Rule: Completion blocked while mandatory checklist items are unticked
        long incompleteMandatoryCount = checklistItemRepository.countByJobAndMandatoryTrueAndCompletedFalse(job);
        if (incompleteMandatoryCount > 0) {
            throw new ValidationException("Cannot complete job: " + incompleteMandatoryCount +
                    " mandatory checklist item(s) are still pending");
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        // Generate random, unguessable proof code if not already present
        if (job.getProofCode() == null || job.getProofCode().isEmpty()) {
            job.setProofCode(generateProofCode());
        }

        Job saved = jobRepository.save(job);

        auditService.log(AuditAction.JOB_COMPLETED, saved.getId(), saved.getTitle(), actor,
                "Job marked completed. Proof code generated.", ip);
        auditService.log(AuditAction.PROOF_GENERATED, saved.getId(), saved.getTitle(), actor,
                "Proof URL activated: /proof/" + saved.getProofCode(), ip);

        return saved;
    }

    @Transactional
    public Job approveJob(Long jobId, String approverName, String comments, User actor, String ip) {
        Job job = getJobById(jobId);
        checkCustomerOwnershipOrManager(job, actor);

        if (!job.getStatus().canTransitionTo(JobStatus.APPROVED)) {
            throw new ValidationException("Only completed jobs can be approved (current status: " + job.getStatus().getLabel() + ")");
        }

        String effectiveName = (approverName != null && !approverName.trim().isEmpty())
                ? approverName.trim()
                : (actor != null ? actor.getFullName() : job.getCustomer().getContactPerson());
        String effectiveEmail = (actor != null) ? actor.getEmail() : job.getCustomer().getEmail();

        Approval approval = new Approval();
        approval.setJob(job);
        approval.setApprovedBy(actor);
        approval.setApproverName(effectiveName);
        approval.setApproverEmail(effectiveEmail);
        approval.setComments(comments != null ? comments.trim() : "");
        approval.setApprovedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        job.setApproval(approval);
        job.setStatus(JobStatus.APPROVED);
        job.setUpdatedAt(LocalDateTime.now());
        Job saved = jobRepository.save(job);

        auditService.log(AuditAction.CUSTOMER_APPROVED, saved.getId(), saved.getTitle(), actor,
                "Customer approved the work. Approver: " + effectiveName, ip);

        return saved;
    }

    @Transactional
    public Job cancelJob(Long jobId, String reason, User actor, String ip) {
        Job job = getJobById(jobId);
        checkManagerOrAdmin(actor);

        if (!job.getStatus().canTransitionTo(JobStatus.CANCELLED)) {
            throw new ValidationException("Cannot cancel job in status " + job.getStatus().getLabel());
        }

        job.setStatus(JobStatus.CANCELLED);
        job.setUpdatedAt(LocalDateTime.now());
        Job saved = jobRepository.save(job);

        auditService.log(AuditAction.JOB_UPDATED, saved.getId(), saved.getTitle(), actor,
                "Job cancelled. Reason: " + (reason != null ? reason : "None provided"), ip);
        return saved;
    }

    @Transactional
    public ChecklistItem toggleChecklistItem(Long itemId, User actor) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Checklist item not found: " + itemId));
        checkTechnicianOwnershipOrManager(item.getJob(), actor);

        item.setCompleted(!item.isCompleted());
        if (item.isCompleted()) {
            item.setCompletedAt(LocalDateTime.now());
            item.setCompletedBy(actor);
        } else {
            item.setCompletedAt(null);
            item.setCompletedBy(null);
        }
        return checklistItemRepository.save(item);
    }

    @Transactional
    public ChecklistItem addChecklistItem(Long jobId, String title, boolean mandatory, User actor) {
        Job job = getJobById(jobId);
        checkTechnicianOwnershipOrManager(job, actor);

        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Checklist title cannot be empty");
        }

        ChecklistItem item = new ChecklistItem(job, title.trim(), mandatory);
        return checklistItemRepository.save(item);
    }

    /* ---------------- Security & Ownership Guards ---------------- */

    public void checkTechnicianOwnershipOrManager(Job job, User actor) {
        if (actor == null) {
            throw new AuthorizationException("Authentication required");
        }
        if (actor.isAdmin() || actor.isManager()) {
            return;
        }
        if (actor.isTechnician()) {
            if (job.getAssignedTechnician() != null && job.getAssignedTechnician().getId().equals(actor.getId())) {
                return;
            }
            throw new AuthorizationException("Access denied: You can only work on jobs assigned to you");
        }
        throw new AuthorizationException("Access denied: Insufficient privileges");
    }

    public void checkCustomerOwnershipOrManager(Job job, User actor) {
        if (actor == null) {
            throw new AuthorizationException("Authentication required");
        }
        if (actor.isAdmin() || actor.isManager()) {
            return;
        }
        if (actor.isCustomer()) {
            if (job.getCustomer().getUser() != null && job.getCustomer().getUser().getId().equals(actor.getId())) {
                return;
            }
            if (job.getCustomer().getEmail().equalsIgnoreCase(actor.getEmail())) {
                return;
            }
            throw new AuthorizationException("Access denied: You can only approve jobs for your account");
        }
        throw new AuthorizationException("Access denied: Insufficient privileges to approve this job");
    }

    public void checkManagerOrAdmin(User actor) {
        if (actor == null || (!actor.isAdmin() && !actor.isManager())) {
            throw new AuthorizationException("Access denied: Action requires Manager or Admin role");
        }
    }

    private String generateProofCode() {
        // High-entropy 16-character alphanumeric string + UUID prefix
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long randomLong = Math.abs(RANDOM.nextLong()) % 10000;
        return String.format("%s%04d", uuidPart, randomLong).toUpperCase();
    }
}


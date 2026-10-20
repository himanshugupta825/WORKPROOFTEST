package com.workproof.model;

import com.workproof.model.enums.JobStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User assignedTechnician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Unguessable secure code generated upon completion for public verification */
    @Column(name = "proof_code", unique = true)
    private String proofCode;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt ASC")
    private List<Evidence> evidenceList = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ChecklistItem> checklistItems = new ArrayList<>();

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private Approval approval;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Job() {}

    public Job(String title, String description, Customer customer, Location location, User createdBy) {
        this.title = title;
        this.description = description;
        this.customer = customer;
        this.location = location;
        this.createdBy = createdBy;
        this.status = JobStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public User getAssignedTechnician() { return assignedTechnician; }
    public void setAssignedTechnician(User assignedTechnician) { this.assignedTechnician = assignedTechnician; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getProofCode() { return proofCode; }
    public void setProofCode(String proofCode) { this.proofCode = proofCode; }

    public List<Evidence> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<Evidence> evidenceList) { this.evidenceList = evidenceList; }

    public List<ChecklistItem> getChecklistItems() { return checklistItems; }
    public void setChecklistItems(List<ChecklistItem> checklistItems) { this.checklistItems = checklistItems; }

    public Approval getApproval() { return approval; }
    public void setApproval(Approval approval) { this.approval = approval; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean canComplete() {
        if (checklistItems == null || checklistItems.isEmpty()) {
            return true;
        }
        for (ChecklistItem item : checklistItems) {
            if (item.isMandatory() && !item.isCompleted()) {
                return false;
            }
        }
        return true;
    }
}


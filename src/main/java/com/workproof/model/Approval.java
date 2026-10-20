package com.workproof.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approver_name", nullable = false)
    private String approverName;

    @Column(name = "approver_email", nullable = false)
    private String approverEmail;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt = LocalDateTime.now();

    public Approval() {}

    public Approval(Job job, User approvedBy, String approverName, String approverEmail, String comments) {
        this.job = job;
        this.approvedBy = approvedBy;
        this.approverName = approverName;
        this.approverEmail = approverEmail;
        this.comments = comments;
        this.approvedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getApproverEmail() { return approverEmail; }
    public void setApproverEmail(String approverEmail) { this.approverEmail = approverEmail; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}


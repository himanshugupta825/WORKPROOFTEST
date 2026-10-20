package com.workproof.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "mandatory", nullable = false)
    private boolean mandatory = true;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    private User completedBy;

    public ChecklistItem() {}

    public ChecklistItem(Job job, String title, boolean mandatory) {
        this.job = job;
        this.title = title;
        this.mandatory = mandatory;
        this.completed = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public User getCompletedBy() { return completedBy; }
    public void setCompletedBy(User completedBy) { this.completedBy = completedBy; }
}


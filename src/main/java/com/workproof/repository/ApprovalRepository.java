package com.workproof.repository;

import com.workproof.model.Approval;
import com.workproof.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    Optional<Approval> findByJob(Job job);
    Optional<Approval> findByJobId(Long jobId);
}


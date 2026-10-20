package com.workproof.repository;

import com.workproof.model.Evidence;
import com.workproof.model.Job;
import com.workproof.model.enums.EvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByJobOrderByUploadedAtAsc(Job job);
    List<Evidence> findByJobAndEvidenceTypeOrderByUploadedAtAsc(Job job, EvidenceType evidenceType);
    long countByJob(Job job);
}


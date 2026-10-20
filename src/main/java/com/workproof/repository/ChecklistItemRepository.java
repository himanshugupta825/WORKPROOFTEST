package com.workproof.repository;

import com.workproof.model.ChecklistItem;
import com.workproof.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByJobOrderByIdAsc(Job job);
    long countByJobAndMandatoryTrueAndCompletedFalse(Job job);
}


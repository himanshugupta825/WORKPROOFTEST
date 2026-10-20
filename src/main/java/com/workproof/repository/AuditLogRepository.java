package com.workproof.repository;

import com.workproof.model.AuditLog;
import com.workproof.model.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
    List<AuditLog> findTop20ByOrderByTimestampDesc();
    List<AuditLog> findByJobIdOrderByTimestampDesc(Long jobId);
    List<AuditLog> findByActionOrderByTimestampDesc(AuditAction action);
}


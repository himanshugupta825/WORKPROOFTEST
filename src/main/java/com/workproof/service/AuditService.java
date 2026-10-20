package com.workproof.service;

import com.workproof.model.AuditLog;
import com.workproof.model.User;
import com.workproof.model.enums.AuditAction;
import com.workproof.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(AuditAction action, Long jobId, String jobTitle, User actor, String details, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setJobId(jobId);
        entry.setJobTitle(jobTitle);
        if (actor != null) {
            entry.setUserId(actor.getId());
            entry.setUserEmail(actor.getEmail());
        }
        entry.setDetails(details);
        entry.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        entry.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsForJob(Long jobId) {
        return auditLogRepository.findByJobIdOrderByTimestampDesc(jobId);
    }
}


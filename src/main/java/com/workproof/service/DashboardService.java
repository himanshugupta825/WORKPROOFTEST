package com.workproof.service;

import com.workproof.dao.DashboardDao;
import com.workproof.dto.JobDashboardStats;
import com.workproof.model.DashboardStats;
import com.workproof.model.User;
import com.workproof.model.enums.JobStatus;
import com.workproof.repository.AuditLogRepository;
import com.workproof.repository.CustomerRepository;
import com.workproof.repository.JobRepository;
import com.workproof.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Supplies the numbers for the role-specific dashboards.
 * Supports both Spring Boot WorkProof platform stats and legacy role metrics.
 */
@Service
public class DashboardService {

    private final DashboardDao dashboardDao = new DashboardDao();

    @Autowired(required = false)
    private JobRepository jobRepository;

    @Autowired(required = false)
    private LocationRepository locationRepository;

    @Autowired(required = false)
    private CustomerRepository customerRepository;

    @Autowired(required = false)
    private AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public JobDashboardStats getStatsForUser(User user) {
        JobDashboardStats stats = new JobDashboardStats();
        if (jobRepository == null) {
            return stats;
        }

        stats.setTotalJobs(jobRepository.count());
        stats.setActiveJobs(jobRepository.countByStatusIn(List.of(JobStatus.ASSIGNED, JobStatus.IN_PROGRESS)));
        stats.setCompletedJobs(jobRepository.countByStatus(JobStatus.COMPLETED));
        stats.setApprovedJobs(jobRepository.countByStatus(JobStatus.APPROVED));

        if (locationRepository != null) {
            stats.setTotalLocations(locationRepository.count());
        }
        if (customerRepository != null) {
            stats.setTotalCustomers(customerRepository.count());
        }
        if (auditLogRepository != null) {
            stats.setRecentActivity(auditLogRepository.findTop20ByOrderByTimestampDesc());
        }

        // Recent jobs for display
        stats.setRecentJobs(jobRepository.findAllByOrderByCreatedAtDesc());

        return stats;
    }

    public DashboardStats statsFor(User user) {
        switch (user.getRole()) {
            case ADMIN:
                return dashboardDao.statsForAdmin();
            case MANAGER:
                return dashboardDao.statsForManager(user.getId());
            case REVIEWER:
                return dashboardDao.statsForReviewer(user.getId());
            case MEMBER:
            default:
                return dashboardDao.statsForMember(user.getId());
        }
    }
}

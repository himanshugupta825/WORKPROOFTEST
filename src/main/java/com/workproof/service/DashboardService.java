package com.workproof.service;

import com.workproof.dao.DashboardDao;
import com.workproof.model.DashboardStats;
import com.workproof.model.User;

/**
 * Supplies the numbers for the role-specific dashboards (Module 2).
 *
 * The service only decides WHICH scope a role gets; DashboardDao runs the actual
 * COUNT queries, so no metric is ever hardcoded in a JSP.
 */
public class DashboardService {

    private final DashboardDao dashboardDao = new DashboardDao();

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

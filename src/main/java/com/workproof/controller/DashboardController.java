package com.workproof.controller;

import com.workproof.dto.JobDashboardStats;
import com.workproof.model.User;
import com.workproof.service.CustomUserDetails;
import com.workproof.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userDetails.getUser();
        JobDashboardStats stats = dashboardService.getStatsForUser(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("stats", stats);
        return "dashboard/index";
    }
}


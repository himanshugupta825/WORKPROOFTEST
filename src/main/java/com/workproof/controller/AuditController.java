package com.workproof.controller;

import com.workproof.model.AuditLog;
import com.workproof.service.AuditService;
import com.workproof.service.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/audit")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String viewAuditLog(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<AuditLog> logs = auditService.getAllAuditLogs();
        model.addAttribute("logs", logs);
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);
        return "audit/list";
    }
}


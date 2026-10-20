package com.workproof.controller;

import com.workproof.dto.CustomerApprovalDto;
import com.workproof.dto.EvidenceUploadDto;
import com.workproof.dto.JobCreateDto;
import com.workproof.exception.ValidationException;
import com.workproof.model.Customer;
import com.workproof.model.Job;
import com.workproof.model.Location;
import com.workproof.model.User;
import com.workproof.model.enums.EvidenceType;
import com.workproof.model.enums.JobStatus;
import com.workproof.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final CustomerService customerService;
    private final LocationService locationService;
    private final UserService userService;
    private final EvidenceService evidenceService;

    public JobController(JobService jobService,
                         CustomerService customerService,
                         LocationService locationService,
                         UserService userService,
                         EvidenceService evidenceService) {
        this.jobService = jobService;
        this.customerService = customerService;
        this.locationService = locationService;
        this.userService = userService;
        this.evidenceService = evidenceService;
    }

    @GetMapping
    public String listJobs(@RequestParam(value = "status", required = false) String statusParam,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        User currentUser = userDetails.getUser();
        JobStatus status = null;
        if (statusParam != null && !statusParam.isEmpty()) {
            try {
                status = JobStatus.valueOf(statusParam.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<Job> jobs = jobService.getJobsForUser(currentUser, status);

        model.addAttribute("jobs", jobs);
        model.addAttribute("currentStatus", statusParam);
        model.addAttribute("allStatuses", JobStatus.values());
        model.addAttribute("currentUser", currentUser);
        return "jobs/list";
    }

    @GetMapping("/create")
    public String createJobForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User currentUser = userDetails.getUser();
        jobService.checkManagerOrAdmin(currentUser);

        if (!model.containsAttribute("jobDto")) {
            model.addAttribute("jobDto", new JobCreateDto());
        }
        populateFormData(model);
        model.addAttribute("currentUser", currentUser);
        return "jobs/create";
    }

    @PostMapping("/create")
    public String handleCreateJob(@Valid @ModelAttribute("jobDto") JobCreateDto jobDto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User currentUser = userDetails.getUser();
        jobService.checkManagerOrAdmin(currentUser);

        if (bindingResult.hasErrors()) {
            populateFormData(model);
            model.addAttribute("currentUser", currentUser);
            return "jobs/create";
        }

        try {
            Customer customer = customerService.getCustomerById(jobDto.getCustomerId())
                    .orElseThrow(() -> new ValidationException("Selected customer does not exist"));
            Location location = locationService.getLocationById(jobDto.getLocationId())
                    .orElseThrow(() -> new ValidationException("Selected location does not exist"));

            User technician = null;
            if (jobDto.getTechnicianId() != null) {
                technician = userService.findUserById(jobDto.getTechnicianId()).orElse(null);
            }

            List<String> checklist = new ArrayList<>();
            if (jobDto.getChecklistItemsText() != null && !jobDto.getChecklistItemsText().trim().isEmpty()) {
                String[] lines = jobDto.getChecklistItemsText().split("\\r?\\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        checklist.add(line.trim());
                    }
                }
            }

            Job created = jobService.createJob(
                    jobDto.getTitle(),
                    jobDto.getDescription(),
                    customer,
                    location,
                    technician,
                    currentUser,
                    checklist,
                    request.getRemoteAddr()
            );

            redirectAttributes.addFlashAttribute("successMessage", "Job created successfully!");
            return "redirect:/jobs/" + created.getId();

        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormData(model);
            model.addAttribute("currentUser", currentUser);
            return "jobs/create";
        }
    }

    @GetMapping("/{id}")
    public String jobDetail(@PathVariable("id") Long id,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        User currentUser = userDetails.getUser();
        Job job = jobService.getJobById(id);

        model.addAttribute("job", job);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("evidenceDto", new EvidenceUploadDto());
        model.addAttribute("approvalDto", new CustomerApprovalDto());
        model.addAttribute("evidenceTypes", EvidenceType.values());
        model.addAttribute("technicians", userService.getAllTechnicians());

        return "jobs/detail";
    }

    @PostMapping("/{id}/start")
    public String startJob(@PathVariable("id") Long id,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            jobService.startJob(id, userDetails.getUser(), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Job started! Status updated to In Progress.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignTechnician(@PathVariable("id") Long id,
                                   @RequestParam("technicianId") Integer technicianId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            User tech = userService.findUserById(technicianId)
                    .orElseThrow(() -> new ValidationException("Technician not found"));
            jobService.assignTechnician(id, tech, userDetails.getUser(), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Technician assigned successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/checklist/{itemId}/toggle")
    public String toggleChecklist(@PathVariable("id") Long id,
                                  @PathVariable("itemId") Long itemId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            jobService.toggleChecklistItem(itemId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Checklist item updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/checklist/add")
    public String addChecklistItem(@PathVariable("id") Long id,
                                   @RequestParam("title") String title,
                                   @RequestParam(value = "mandatory", defaultValue = "false") boolean mandatory,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            jobService.addChecklistItem(id, title, mandatory, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Checklist item added.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/evidence/upload")
    public String uploadEvidence(@PathVariable("id") Long id,
                                 @ModelAttribute("evidenceDto") EvidenceUploadDto evidenceDto,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            Job job = jobService.getJobById(id);
            jobService.checkTechnicianOwnershipOrManager(job, userDetails.getUser());

            EvidenceType type = EvidenceType.valueOf(evidenceDto.getEvidenceType());
            evidenceService.storeEvidence(
                    job,
                    type,
                    evidenceDto.getCaption(),
                    evidenceDto.getFile(),
                    userDetails.getUser(),
                    request.getRemoteAddr()
            );

            redirectAttributes.addFlashAttribute("successMessage", "Evidence uploaded successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/complete")
    public String completeJob(@PathVariable("id") Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        try {
            Job completed = jobService.completeJob(id, userDetails.getUser(), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Job completed! Proof code generated: " + completed.getProofCode());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approveJob(@PathVariable("id") Long id,
                             @ModelAttribute("approvalDto") CustomerApprovalDto approvalDto,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            jobService.approveJob(id, approvalDto.getApproverName(), approvalDto.getComments(),
                    userDetails.getUser(), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Job successfully approved! Thank you.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelJob(@PathVariable("id") Long id,
                            @RequestParam(value = "reason", required = false) String reason,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            jobService.cancelJob(id, reason, userDetails.getUser(), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "Job cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/jobs/" + id;
    }

    private void populateFormData(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("locations", locationService.getAllLocations());
        model.addAttribute("technicians", userService.getAllTechnicians());
    }
}


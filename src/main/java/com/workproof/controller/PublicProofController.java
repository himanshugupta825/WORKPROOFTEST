package com.workproof.controller;

import com.workproof.exception.NotFoundException;
import com.workproof.model.Evidence;
import com.workproof.model.Job;
import com.workproof.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/proof")
public class PublicProofController {

    private final JobService jobService;

    public PublicProofController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{code}")
    public String viewPublicProof(@PathVariable("code") String code, Model model) {
        Job job = jobService.getJobByProofCode(code)
                .orElseThrow(() -> new NotFoundException("No verification proof found for code: " + code));

        // Group evidence for clear Before / After comparison
        List<Evidence> beforePhotos = job.getEvidenceList().stream()
                .filter(Evidence::isBeforePhoto)
                .collect(Collectors.toList());

        List<Evidence> afterPhotos = job.getEvidenceList().stream()
                .filter(Evidence::isAfterPhoto)
                .collect(Collectors.toList());

        List<Evidence> otherEvidence = job.getEvidenceList().stream()
                .filter(e -> !e.isBeforePhoto() && !e.isAfterPhoto())
                .collect(Collectors.toList());

        model.addAttribute("job", job);
        model.addAttribute("proofCode", code);
        model.addAttribute("beforePhotos", beforePhotos);
        model.addAttribute("afterPhotos", afterPhotos);
        model.addAttribute("otherEvidence", otherEvidence);

        return "proof/public";
    }
}


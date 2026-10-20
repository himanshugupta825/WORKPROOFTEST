package com.workproof.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class JobCreateDto {

    @NotBlank(message = "Job title is required")
    @Size(min = 3, max = 150, message = "Job title must be between 3 and 150 characters")
    private String title;

    private String description;

    @NotNull(message = "Customer selection is required")
    private Long customerId;

    @NotNull(message = "Location selection is required")
    private Long locationId;

    private Integer technicianId;

    private String checklistItemsText;

    public JobCreateDto() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Integer getTechnicianId() { return technicianId; }
    public void setTechnicianId(Integer technicianId) { this.technicianId = technicianId; }

    public String getChecklistItemsText() { return checklistItemsText; }
    public void setChecklistItemsText(String checklistItemsText) { this.checklistItemsText = checklistItemsText; }
}


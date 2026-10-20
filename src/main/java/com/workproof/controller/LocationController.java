package com.workproof.controller;

import com.workproof.dto.LocationDto;
import com.workproof.exception.NotFoundException;
import com.workproof.model.Location;
import com.workproof.model.User;
import com.workproof.service.CustomUserDetails;
import com.workproof.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String listLocations(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Location> locations = locationService.getAllLocations();
        model.addAttribute("locations", locations);
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);
        return "locations/list";
    }

    @GetMapping("/create")
    public String createForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (!model.containsAttribute("locationDto")) {
            model.addAttribute("locationDto", new LocationDto());
        }
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);
        return "locations/form";
    }

    @PostMapping("/create")
    public String handleCreate(@Valid @ModelAttribute("locationDto") LocationDto locationDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "locations/form";
        }

        Location loc = new Location();
        loc.setName(locationDto.getName().trim());
        loc.setAddress(locationDto.getAddress().trim());
        loc.setCity(locationDto.getCity());
        loc.setPostalCode(locationDto.getPostalCode());
        loc.setContactName(locationDto.getContactName());
        loc.setContactPhone(locationDto.getContactPhone());

        locationService.saveLocation(loc);
        redirectAttributes.addFlashAttribute("successMessage", "Location created successfully!");
        return "redirect:/locations";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        Location location = locationService.getLocationById(id)
                .orElseThrow(() -> new NotFoundException("Location not found: " + id));

        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        dto.setCity(location.getCity());
        dto.setPostalCode(location.getPostalCode());
        dto.setContactName(location.getContactName());
        dto.setContactPhone(location.getContactPhone());

        model.addAttribute("locationDto", dto);
        model.addAttribute("isEdit", true);
        model.addAttribute("currentUser", userDetails != null ? userDetails.getUser() : null);
        return "locations/form";
    }

    @PostMapping("/{id}/edit")
    public String handleEdit(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("locationDto") LocationDto locationDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "locations/form";
        }

        Location location = locationService.getLocationById(id)
                .orElseThrow(() -> new NotFoundException("Location not found: " + id));

        location.setName(locationDto.getName().trim());
        location.setAddress(locationDto.getAddress().trim());
        location.setCity(locationDto.getCity());
        location.setPostalCode(locationDto.getPostalCode());
        location.setContactName(locationDto.getContactName());
        location.setContactPhone(locationDto.getContactPhone());

        locationService.saveLocation(location);
        redirectAttributes.addFlashAttribute("successMessage", "Location updated successfully!");
        return "redirect:/locations";
    }
}


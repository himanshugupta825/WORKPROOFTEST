package com.workproof.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LocationDto {

    private Long id;

    @NotBlank(message = "Location name is required")
    @Size(min = 2, max = 120, message = "Location name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    private String city;
    private String postalCode;
    private String contactName;
    private String contactPhone;

    public LocationDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}


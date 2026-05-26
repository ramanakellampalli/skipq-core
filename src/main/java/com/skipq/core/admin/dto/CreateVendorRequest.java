package com.skipq.core.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateVendorRequest(
        @NotBlank String vendorName,
        @NotBlank @Email String email,
        @NotBlank String ownerName,
        @NotNull @Min(1) Integer defaultPrepTime,
        UUID campusId,                              // nullable — null means general vendor
        @Size(max = 100) String city,               // required when campusId is null
        @NotBlank @Size(max = 20) String ownerPhone,
        @NotBlank @Size(max = 20) String contactPhone
) {}

package com.skipq.core.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVendorRequest(
        @NotBlank String vendorName,
        @NotBlank @Email String email,
        @NotBlank String ownerName,
        @NotNull @Min(1) Integer defaultPrepTime,
        @NotNull UUID campusId
) {}

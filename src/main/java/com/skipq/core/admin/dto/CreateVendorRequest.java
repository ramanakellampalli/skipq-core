package com.skipq.core.admin.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateVendorRequest(
        @NotBlank String vendorName,
        @NotBlank @Email String email,
        @NotBlank String ownerName,
        @NotNull @Min(1) Integer defaultPrepTime,
        UUID campusId,
        @Size(max = 100) String city,
        @NotBlank @Size(max = 20) String ownerPhone,

        @NotBlank String businessName,
        @NotBlank @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "Invalid PAN format") String pan,
        @NotBlank String bankAccount,
        @NotBlank @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "Invalid IFSC format") String ifsc,

        boolean gstRegistered,
        @Pattern(regexp = "[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]",
                 message = "Invalid GSTIN format") String gstin,

        @PositiveOrZero java.math.BigDecimal subscriptionMonthlyPrice
) {}

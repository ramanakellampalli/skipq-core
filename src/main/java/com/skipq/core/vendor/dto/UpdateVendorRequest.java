package com.skipq.core.vendor.dto;

import jakarta.validation.constraints.Min;

public record UpdateVendorRequest(
        Boolean isOpen,
        @Min(1) Integer prepTime
) {}

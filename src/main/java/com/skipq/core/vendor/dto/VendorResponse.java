package com.skipq.core.vendor.dto;

import java.util.UUID;

public record VendorResponse(
        UUID id,
        String name,
        boolean isOpen,
        int prepTime
) {}

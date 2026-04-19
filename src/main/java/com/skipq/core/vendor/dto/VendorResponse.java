package com.skipq.core.vendor.dto;

import java.util.UUID;

public record VendorResponse(
        UUID id,
        String name,
        boolean isOpen,
        int prepTime,
        String businessName,
        boolean gstRegistered,
        String gstin,
        boolean kycApproved,
        UUID campusId,
        String campusName
) {}

package com.skipq.core.vendor.dto;

import com.skipq.core.common.AccountStatus;
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
        String campusName,
        AccountStatus accountStatus,
        String suspensionNote,
        String logoUrl,
        String city,
        String phone
) {}

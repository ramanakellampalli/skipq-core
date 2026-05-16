package com.skipq.core.admin.dto;

import com.skipq.core.common.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVendorStatusRequest(
        @NotNull AccountStatus status,
        String note
) {}

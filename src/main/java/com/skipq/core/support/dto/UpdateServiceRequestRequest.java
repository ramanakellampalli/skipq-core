package com.skipq.core.support.dto;

import com.skipq.core.support.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateServiceRequestRequest(
        @NotNull ServiceRequestStatus status,
        String adminResponse,
        String adminNotes
) {}

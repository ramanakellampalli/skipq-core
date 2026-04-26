package com.skipq.core.support.dto;

import com.skipq.core.support.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceRequestRequest(
        @NotNull ServiceRequestType type,
        @NotBlank String description
) {}

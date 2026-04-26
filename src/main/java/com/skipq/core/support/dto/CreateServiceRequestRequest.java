package com.skipq.core.support.dto;

import com.skipq.core.support.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequestRequest(
        @NotNull ServiceRequestType type,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank String description
) {}

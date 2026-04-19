package com.skipq.core.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCampusRequest(
        @NotBlank String name,
        @NotBlank String emailDomain
) {}

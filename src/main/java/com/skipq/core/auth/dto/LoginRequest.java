package com.skipq.core.auth.dto;

import com.skipq.core.common.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        UserRole role
) {}

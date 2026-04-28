package com.skipq.core.auth.dto;

import com.skipq.core.common.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordRequest(
        @Email @NotBlank String email,
        @NotNull UserRole role
) {}

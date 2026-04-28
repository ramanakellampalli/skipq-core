package com.skipq.core.auth.dto;

import com.skipq.core.common.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotNull UserRole role,
        @NotBlank String otp,
        @NotBlank @Size(min = 8) String newPassword
) {}

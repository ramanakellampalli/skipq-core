package com.skipq.core.auth.dto;

import com.skipq.core.common.UserRole;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String name,
        String email,
        UserRole role
) {}

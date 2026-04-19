package com.skipq.core.student.dto;

import java.util.UUID;

public record StudentProfile(
        UUID id,
        String name,
        String email,
        UUID campusId,
        String campusName
) {}

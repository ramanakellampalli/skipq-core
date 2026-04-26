package com.skipq.core.support.dto;

import com.skipq.core.common.UserRole;
import com.skipq.core.support.ServiceRequestStatus;
import com.skipq.core.support.ServiceRequestType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminServiceRequestResponse(
        UUID id,
        UserRole role,
        String userName,
        String userEmail,
        ServiceRequestType type,
        String subject,
        String description,
        ServiceRequestStatus status,
        String adminResponse,
        String adminNotes,
        LocalDateTime adminRespondedAt,
        LocalDateTime createdAt
) {}

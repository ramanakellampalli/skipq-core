package com.skipq.core.support.dto;

import com.skipq.core.support.ServiceRequestStatus;
import com.skipq.core.support.ServiceRequestType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceRequestResponse(
        UUID id,
        ServiceRequestType type,
        String subject,
        ServiceRequestStatus status,
        String adminResponse,
        LocalDateTime adminRespondedAt,
        LocalDateTime createdAt
) {}

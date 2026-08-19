package com.skipq.core.subscription.dto;

import com.skipq.core.subscription.AdminSubscriptionStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateSubscriptionRequest(
        @PositiveOrZero BigDecimal monthlyPrice,
        AdminSubscriptionStatus status
) {}

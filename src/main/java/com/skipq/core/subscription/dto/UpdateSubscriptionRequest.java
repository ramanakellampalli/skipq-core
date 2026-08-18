package com.skipq.core.subscription.dto;

import com.skipq.core.vendor.SubscriptionStatus;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateSubscriptionRequest(
        @PositiveOrZero BigDecimal monthlyPrice,
        SubscriptionStatus status
) {}

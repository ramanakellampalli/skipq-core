package com.skipq.core.subscription.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateSubscriptionRequest(
        @PositiveOrZero BigDecimal monthlyPrice
) {}

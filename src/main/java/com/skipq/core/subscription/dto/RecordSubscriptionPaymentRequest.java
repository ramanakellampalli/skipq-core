package com.skipq.core.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordSubscriptionPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate paidForMonth,
        String paymentReference,
        @NotNull LocalDate paidOn,
        String adminNote
) {}

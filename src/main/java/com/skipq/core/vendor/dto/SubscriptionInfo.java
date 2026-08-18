package com.skipq.core.vendor.dto;

import com.skipq.core.vendor.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionInfo(
        SubscriptionStatus status,
        BigDecimal monthlyPrice,
        LocalDate paidThrough,
        String lastPaymentReference
) {}

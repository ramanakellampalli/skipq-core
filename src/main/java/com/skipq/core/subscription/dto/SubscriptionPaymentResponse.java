package com.skipq.core.subscription.dto;

import com.skipq.core.subscription.SubscriptionPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionPaymentResponse(
        UUID id,
        BigDecimal amount,
        String paymentReference,
        LocalDate paidForMonth,
        LocalDate paidOn,
        String adminNote,
        LocalDateTime createdAt
) {
    public static SubscriptionPaymentResponse from(SubscriptionPayment p) {
        return new SubscriptionPaymentResponse(
                p.getId(), p.getAmount(), p.getPaymentReference(),
                p.getPaidForMonth(), p.getPaidOn(), p.getAdminNote(), p.getCreatedAt()
        );
    }
}

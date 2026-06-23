package com.skipq.core.settlement.dto;

import com.skipq.core.settlement.PayoutStatus;
import com.skipq.core.settlement.VendorPayout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VendorPayoutSummary(
        UUID id,
        BigDecimal amount,
        LocalDateTime settlementCutoffAt,
        PayoutStatus status,
        String payoutReference,
        LocalDateTime createdAt
) {
    public static VendorPayoutSummary from(VendorPayout p) {
        return new VendorPayoutSummary(
                p.getId(),
                p.getAmount(),
                p.getSettlementCutoffAt(),
                p.getStatus(),
                p.getPayoutReference(),
                p.getCreatedAt()
        );
    }
}

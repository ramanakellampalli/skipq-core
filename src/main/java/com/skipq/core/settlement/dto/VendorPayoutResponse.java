package com.skipq.core.settlement.dto;

import com.skipq.core.settlement.PayoutStatus;
import com.skipq.core.settlement.VendorPayout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VendorPayoutResponse(
        UUID id,
        UUID vendorId,
        String vendorName,
        BigDecimal amount,
        LocalDateTime settlementStartAt,
        LocalDateTime settlementCutoffAt,
        PayoutStatus status,
        String payoutReference,
        String adminNote,
        LocalDateTime createdAt
) {
    public static VendorPayoutResponse from(VendorPayout p) {
        return new VendorPayoutResponse(
                p.getId(),
                p.getVendor().getId(),
                p.getVendor().getName(),
                p.getAmount(),
                p.getSettlementStartAt(),
                p.getSettlementCutoffAt(),
                p.getStatus(),
                p.getPayoutReference(),
                p.getAdminNote(),
                p.getCreatedAt()
        );
    }
}

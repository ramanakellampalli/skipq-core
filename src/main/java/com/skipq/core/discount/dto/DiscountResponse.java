package com.skipq.core.discount.dto;

import com.skipq.core.discount.Discount;
import com.skipq.core.discount.DiscountScope;
import com.skipq.core.discount.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DiscountResponse(
        UUID          id,
        String        name,
        DiscountType  type,
        BigDecimal    value,
        DiscountScope scope,
        boolean       active,
        int           priority,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int           itemCount,
        LocalDateTime createdAt
) {
    public static DiscountResponse from(Discount d) {
        return new DiscountResponse(
                d.getId(),
                d.getName(),
                d.getType(),
                d.getValue(),
                d.getScope(),
                d.isActive(),
                d.getPriority(),
                d.getStartsAt(),
                d.getEndsAt(),
                d.getMenuItems().size(),
                d.getCreatedAt()
        );
    }
}

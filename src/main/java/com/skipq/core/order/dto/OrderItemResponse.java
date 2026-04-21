package com.skipq.core.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID menuItemId,
        UUID variantId,
        String name,
        String variantLabel,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}

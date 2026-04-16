package com.skipq.core.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID menuItemId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}

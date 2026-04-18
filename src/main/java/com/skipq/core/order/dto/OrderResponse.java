package com.skipq.core.order.dto;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID vendorId,
        String vendorName,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal subtotal,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal taxAmount,
        BigDecimal platformFee,
        BigDecimal paymentTerminalFee,
        BigDecimal totalServiceFee,
        BigDecimal totalAmount,
        LocalDateTime estimatedReadyAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {}

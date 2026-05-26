package com.skipq.core.order.dto;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.OrderType;
import com.skipq.core.common.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        CustomerInfo customer,
        VendorInfo vendor,
        OrderState state,
        Pricing pricing,
        Timeline timeline,
        List<OrderItemResponse> items
) {
    public record CustomerInfo(String name, String phone) {}

    public record VendorInfo(UUID id, String name) {}

    public record OrderState(OrderStatus orderStatus, PaymentStatus paymentStatus) {}

    public record Pricing(
            BigDecimal subtotal,
            TaxBreakdown tax,
            Fees fees,
            BigDecimal totalAmount
    ) {}

    public record TaxBreakdown(
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal totalTax
    ) {}

    public record Fees(
            BigDecimal platformFee,
            BigDecimal totalServiceFee
    ) {}

    public record Timeline(LocalDateTime createdAt, LocalDateTime estimatedReadyAt, OrderType orderType, LocalDateTime scheduledPickupAt) {}
}

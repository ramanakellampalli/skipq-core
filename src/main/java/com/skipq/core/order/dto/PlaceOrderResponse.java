package com.skipq.core.order.dto;

import java.util.UUID;

public record PlaceOrderResponse(
        UUID orderId,
        String razorpayOrderId,
        long razorpayAmountPaise,
        String razorpayKeyId
) {}

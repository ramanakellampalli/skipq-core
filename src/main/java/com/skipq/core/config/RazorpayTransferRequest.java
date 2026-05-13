package com.skipq.core.config;

public record RazorpayTransferRequest(
        String linkedAccountId,
        long   amountPaise
) {}

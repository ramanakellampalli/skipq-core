package com.skipq.core.admin.dto;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalOrdersToday,
        long activeVendors,
        long ordersInProgress,
        BigDecimal revenueToday
) {}

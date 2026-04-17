package com.skipq.core.order.dto;

import java.math.BigDecimal;

public interface OrderStatsProjection {
    Long getTotalOrders();
    BigDecimal getRevenue();
    Long getInProgress();
}

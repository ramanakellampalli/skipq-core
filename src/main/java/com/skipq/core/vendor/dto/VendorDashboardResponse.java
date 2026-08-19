package com.skipq.core.vendor.dto;

import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.settlement.dto.VendorPayoutSummary;
import com.skipq.core.subscription.dto.SubscriptionPaymentResponse;
import com.skipq.core.support.dto.ServiceRequestResponse;

import java.math.BigDecimal;
import java.util.List;

public record VendorDashboardResponse(
        VendorResponse profile,
        List<OrderResponse> activeOrders,
        List<OrderResponse> pastOrders,
        List<MenuItemResponse> items,
        List<ServiceRequestResponse> serviceRequests,
        BigDecimal availableBalance,
        List<VendorPayoutSummary> recentPayouts,
        List<SubscriptionPaymentResponse> subscriptionPayments
) {}

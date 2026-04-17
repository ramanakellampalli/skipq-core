package com.skipq.core.vendor.dto;

import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.order.dto.OrderResponse;

import java.util.List;

public record VendorDashboardResponse(
        VendorResponse profile,
        List<OrderResponse> activeOrders,
        List<OrderResponse> pastOrders,
        List<MenuItemResponse> menuItems
) {}

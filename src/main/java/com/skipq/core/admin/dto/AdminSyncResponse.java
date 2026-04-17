package com.skipq.core.admin.dto;

import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.vendor.dto.VendorResponse;

import java.util.List;

public record AdminSyncResponse(
        AdminStatsResponse stats,
        List<VendorResponse> vendors,
        List<OrderResponse> orders
) {}

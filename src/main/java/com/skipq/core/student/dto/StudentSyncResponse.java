package com.skipq.core.student.dto;

import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.vendor.dto.VendorResponse;

import java.util.List;

public record StudentSyncResponse(
        StudentProfile profile,
        List<VendorResponse> vendors,
        OrderResponse activeOrder,
        List<OrderResponse> pastOrders
) {}

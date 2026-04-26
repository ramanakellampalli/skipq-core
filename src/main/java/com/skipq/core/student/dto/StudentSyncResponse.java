package com.skipq.core.student.dto;

import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.support.dto.ServiceRequestResponse;
import com.skipq.core.vendor.dto.VendorResponse;

import java.util.List;
import java.util.Map;

public record StudentSyncResponse(
        StudentProfile profile,
        List<VendorResponse> vendors,
        OrderResponse activeOrder,
        List<OrderResponse> pastOrders,
        Map<String, List<String>> vendorImages,
        List<ServiceRequestResponse> serviceRequests
) {}

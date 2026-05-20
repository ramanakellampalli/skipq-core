package com.skipq.core.order;

import com.skipq.core.order.dto.OrderItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return toResponse(order, order.getItems());
    }

    public OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> new OrderItemResponse(
                        i.getMenuItem().getId(),
                        i.getVariant() != null ? i.getVariant().getId() : null,
                        i.getMenuItem().getName(),
                        i.getVariantLabel(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();

        var vendorInfo = new OrderResponse.VendorInfo(order.getVendor().getId(), order.getVendor().getName());
        var state      = new OrderResponse.OrderState(order.getStatus(), order.getPaymentStatus());
        var tax        = new OrderResponse.TaxBreakdown(order.getCgst(), order.getSgst(), order.getIgst(), order.getTaxAmount());
        var fees       = new OrderResponse.Fees(order.getPlatformFee(), order.getTotalServiceFee());
        var pricing    = new OrderResponse.Pricing(order.getSubtotal(), tax, fees, order.getTotalAmount());
        var timeline   = new OrderResponse.Timeline(order.getCreatedAt(), order.getEstimatedReadyAt(), order.getOrderType(), order.getScheduledPickupAt());

        return new OrderResponse(order.getId(), vendorInfo, state, pricing, timeline, itemResponses);
    }
}

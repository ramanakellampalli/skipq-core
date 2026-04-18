package com.skipq.core.student;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.order.Order;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.order.dto.OrderItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.student.dto.StudentSyncResponse;
import com.skipq.core.vendor.VendorService;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final Set<OrderStatus> ACTIVE_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.ACCEPTED,
            OrderStatus.PREPARING, OrderStatus.READY
    );

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final VendorService vendorService;

    @Transactional(readOnly = true)
    public StudentSyncResponse sync(String email) {
        // Query 1: all vendors, open first
        List<VendorResponse> vendors = vendorService.getAllVendors();

        // Query 2: student's orders with items in one JOIN FETCH
        List<Order> orders = orderRepository.findAllByUserEmailWithItems(email);

        List<OrderResponse> activeOrders = orders.stream()
                .filter(o -> ACTIVE_STATUSES.contains(o.getStatus()))
                .map(this::toResponse)
                .toList();

        List<OrderResponse> pastOrders = orders.stream()
                .filter(o -> !ACTIVE_STATUSES.contains(o.getStatus()))
                .map(this::toResponse)
                .toList();

        // Most recent active order, or null
        OrderResponse activeOrder = activeOrders.isEmpty() ? null : activeOrders.get(0);

        return new StudentSyncResponse(vendors, activeOrder, pastOrders);
    }

    public List<MenuItemResponse> getAvailableMenu(java.util.UUID vendorId) {
        return menuItemRepository.findAllByVendorIdAndIsAvailableTrue(vendorId)
                .stream()
                .map(m -> new MenuItemResponse(m.getId(), m.getName(), m.getPrice(), m.isAvailable()))
                .toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getMenuItem().getId(),
                        i.getMenuItem().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getVendor().getId(),
                order.getVendor().getName(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getEstimatedReadyAt(),
                order.getCreatedAt(),
                items
        );
    }
}

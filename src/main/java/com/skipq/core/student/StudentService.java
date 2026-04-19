package com.skipq.core.student;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
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
import java.util.UUID;

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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StudentSyncResponse sync(String email) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        List<VendorResponse> vendors = student.getCampus() != null
                ? vendorService.getVendorsByCampus(student.getCampus())
                : vendorService.getAllVendors();

        List<Order> orders = orderRepository.findAllByUserEmailWithItems(email);

        List<OrderResponse> activeOrders = orders.stream()
                .filter(o -> ACTIVE_STATUSES.contains(o.getStatus()))
                .map(this::toResponse)
                .toList();

        List<OrderResponse> pastOrders = orders.stream()
                .filter(o -> !ACTIVE_STATUSES.contains(o.getStatus()))
                .map(this::toResponse)
                .toList();

        OrderResponse activeOrder = activeOrders.isEmpty() ? null : activeOrders.get(0);

        return new StudentSyncResponse(vendors, activeOrder, pastOrders);
    }

    public List<MenuItemResponse> getAvailableMenu(UUID vendorId) {
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

        var vendorInfo = new OrderResponse.VendorInfo(order.getVendor().getId(), order.getVendor().getName());
        var state      = new OrderResponse.OrderState(order.getStatus(), order.getPaymentStatus());
        var tax        = new OrderResponse.TaxBreakdown(order.getCgst(), order.getSgst(), order.getIgst(), order.getTaxAmount());
        var fees       = new OrderResponse.Fees(order.getPlatformFee(), order.getPaymentTerminalFee(), order.getTotalServiceFee());
        var pricing    = new OrderResponse.Pricing(order.getSubtotal(), tax, fees, order.getTotalAmount());
        var timeline   = new OrderResponse.Timeline(order.getCreatedAt(), order.getEstimatedReadyAt());
        return new OrderResponse(order.getId(), vendorInfo, state, pricing, timeline, items);
    }
}

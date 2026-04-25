package com.skipq.core.student;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.menu.MenuItemService;
import com.skipq.core.student.dto.StudentMenuResponse;
import com.skipq.core.order.Order;
import com.skipq.core.order.OrderItemRepository;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.order.dto.OrderItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.student.dto.StudentProfile;
import com.skipq.core.student.dto.StudentSyncResponse;
import com.skipq.core.config.VendorImageService;
import com.skipq.core.vendor.VendorService;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final Set<OrderStatus> ACTIVE_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.ACCEPTED,
            OrderStatus.PREPARING, OrderStatus.READY
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemService menuItemService;
    private final VendorService vendorService;
    private final UserRepository userRepository;
    private final VendorImageService vendorImageService;

    @Transactional(readOnly = true)
    public StudentSyncResponse sync(UUID userId) {
        User student = userRepository.findByIdWithCampus(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        var campus = student.getCampus();
        StudentProfile profile = new StudentProfile(
                student.getId(),
                student.getName(),
                student.getEmail(),
                campus != null ? campus.getId() : null,
                campus != null ? campus.getName() : null
        );

        List<VendorResponse> vendors = campus != null
                ? vendorService.getVendorsByCampus(campus)
                : vendorService.getAllVendors();

        List<Order> orders = orderRepository.findAllByUserIdWithItems(userId);

        List<OrderResponse> activeOrders = orders.stream()
                .filter(o -> ACTIVE_STATUSES.contains(o.getStatus()))
                .map(this::toResponse)
                .toList();

        List<OrderResponse> pastOrders = orders.stream()
                .filter(o -> !ACTIVE_STATUSES.contains(o.getStatus()))
                .map(this::toResponse)
                .toList();

        OrderResponse activeOrder = activeOrders.isEmpty() ? null : activeOrders.get(0);

        Map<String, List<String>> vendorImages = vendors.stream()
                .collect(Collectors.toMap(
                        v -> v.id().toString(),
                        v -> vendorImageService.getImagesForVendor(v.id())
                ));

        return new StudentSyncResponse(profile, vendors, activeOrder, pastOrders, vendorImages);
    }

    public StudentMenuResponse getAvailableMenu(UUID vendorId) {
        return menuItemService.getAvailableMenuStructured(vendorId);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        List<Order> orders = orderRepository.findAllByUserId(userId);
        orderItemRepository.deleteAllByOrderIn(orders);
        orderRepository.deleteAll(orders);
        userRepository.deleteById(userId);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getMenuItem().getId(),
                        i.getVariant() != null ? i.getVariant().getId() : null,
                        i.getMenuItem().getName(),
                        i.getVariantLabel(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();

        var vendorInfo = new OrderResponse.VendorInfo(order.getVendor().getId(), order.getVendor().getName());
        var state      = new OrderResponse.OrderState(order.getStatus(), order.getPaymentStatus());
        var tax        = new OrderResponse.TaxBreakdown(order.getCgst(), order.getSgst(), order.getIgst(), order.getTaxAmount());
        var fees       = new OrderResponse.Fees(order.getPlatformFee(), order.getTotalServiceFee());
        var pricing    = new OrderResponse.Pricing(order.getSubtotal(), tax, fees, order.getTotalAmount());
        var timeline   = new OrderResponse.Timeline(order.getCreatedAt(), order.getEstimatedReadyAt());
        return new OrderResponse(order.getId(), vendorInfo, state, pricing, timeline, items);
    }
}

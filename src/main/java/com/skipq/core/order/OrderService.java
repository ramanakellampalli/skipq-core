package com.skipq.core.order;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.PaymentStatus;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.config.AblyService;
import com.skipq.core.order.dto.*;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final MenuItemRepository menuItemRepository;
    private final AblyService ablyService;

    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Vendor vendor = vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        if (!vendor.isOpen()) {
            throw new IllegalStateException("Vendor is currently closed");
        }

        if (user.getRole() == com.skipq.core.common.UserRole.VENDOR) {
            vendorRepository.findByUserEmail(email).ifPresent(ownVendor -> {
                if (ownVendor.getId().equals(vendor.getId())) {
                    throw new IllegalArgumentException("You cannot place an order at your own store");
                }
            });
        }

        List<OrderItem> orderItems = request.items().stream().map(itemReq -> {
            MenuItem menuItem = menuItemRepository.findById(itemReq.menuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + itemReq.menuItemId()));

            if (!menuItem.isAvailable()) {
                throw new IllegalStateException("Menu item is not available: " + menuItem.getName());
            }

            if (!menuItem.getVendor().getId().equals(vendor.getId())) {
                throw new IllegalArgumentException("Menu item does not belong to this vendor");
            }

            return OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(itemReq.quantity())
                    .unitPrice(menuItem.getPrice())
                    .build();
        }).toList();

        BigDecimal subtotal = orderItems.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rate025 = new BigDecimal("0.025");
        BigDecimal rate03  = new BigDecimal("0.03");
        BigDecimal rate02  = new BigDecimal("0.02");

        BigDecimal cgst = vendor.isGstRegistered() ? subtotal.multiply(rate025).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sgst = vendor.isGstRegistered() ? subtotal.multiply(rate025).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO; // inter-state not implemented yet
        BigDecimal taxAmount = cgst.add(sgst).add(igst);

        BigDecimal platformFee        = subtotal.multiply(rate03).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal paymentTerminalFee = subtotal.multiply(rate02).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalServiceFee    = platformFee.add(paymentTerminalFee);

        BigDecimal totalAmount = subtotal.add(taxAmount).add(totalServiceFee);

        Order order = Order.builder()
                .user(user)
                .vendor(vendor)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal)
                .cgst(cgst)
                .sgst(sgst)
                .igst(igst)
                .taxAmount(taxAmount)
                .platformFee(platformFee)
                .paymentTerminalFee(paymentTerminalFee)
                .totalServiceFee(totalServiceFee)
                .totalAmount(totalAmount)
                .estimatedReadyAt(LocalDateTime.now().plusMinutes(vendor.getPrepTime()))
                .build();

        orderRepository.save(order);

        orderItems.forEach(item -> item.setOrder(order));
        orderItemRepository.saveAll(orderItems);

        OrderResponse response = toResponse(order, orderItems);
        ablyService.publish("vendor:" + vendor.getId(), "order", response);
        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String email, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Order not found");
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        return toResponse(order, items);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return orderRepository.findAllByUserId(user.getId()).stream()
                .map(order -> toResponse(order, orderItemRepository.findAllByOrderId(order.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getVendorOrders(String email) {
        Vendor vendor = vendorRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        return orderRepository.findAllByVendorId(vendor.getId()).stream()
                .map(order -> toResponse(order, orderItemRepository.findAllByOrderId(order.getId())))
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(String email, UUID orderId, OrderStatus newStatus) {
        Vendor vendor = vendorRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getVendor().getId().equals(vendor.getId())) {
            throw new IllegalArgumentException("Order does not belong to your store");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        OrderResponse response = toResponse(order, items);

        ablyService.publish("vendor:" + vendor.getId(), "order", response);
        ablyService.publish("order:" + order.getId(), "status", response);

        return response;
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> new OrderItemResponse(
                        i.getMenuItem().getId(),
                        i.getMenuItem().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList();

        var vendor = new OrderResponse.VendorInfo(
                order.getVendor().getId(),
                order.getVendor().getName()
        );

        var state = new OrderResponse.OrderState(
                order.getStatus(),
                order.getPaymentStatus()
        );

        var tax = new OrderResponse.TaxBreakdown(
                order.getCgst(),
                order.getSgst(),
                order.getIgst(),
                order.getTaxAmount()
        );

        var fees = new OrderResponse.Fees(
                order.getPlatformFee(),
                order.getPaymentTerminalFee(),
                order.getTotalServiceFee()
        );

        var pricing = new OrderResponse.Pricing(
                order.getSubtotal(),
                tax,
                fees,
                order.getTotalAmount()
        );

        var timeline = new OrderResponse.Timeline(
                order.getCreatedAt(),
                order.getEstimatedReadyAt()
        );

        return new OrderResponse(
                order.getId(),
                vendor,
                state,
                pricing,
                timeline,
                itemResponses
        );
    }
}

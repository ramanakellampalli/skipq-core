package com.skipq.core.order;

import com.razorpay.RazorpayException;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.OrderType;
import com.skipq.core.common.PaymentStatus;
import com.skipq.core.config.AblyService;
import com.skipq.core.config.FcmService;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.config.RazorpayTransferRequest;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuVariant;
import com.skipq.core.menu.MenuVariantRepository;
import com.skipq.core.order.dto.*;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal GST_RATE      = new BigDecimal("0.025");
    private static final BigDecimal PLATFORM_RATE = new BigDecimal("0.03");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuVariantRepository menuVariantRepository;
    private final AblyService ablyService;
    private final FcmService fcmService;
    private final RazorpayService razorpayService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${scheduling.window.start:10:00}")
    private String schedulingWindowStart;

    @Value("${scheduling.window.end:17:00}")
    private String schedulingWindowEnd;

    @Value("${scheduling.min-lead-minutes:30}")
    private int minLeadMinutes;

    @Transactional
    public PlaceOrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Vendor vendor = vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));

        if (!vendor.isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor is currently closed");
        }

        if (user.getCampus() != null && vendor.getCampus() != null && !user.getCampus().getId().equals(vendor.getCampus().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This vendor does not serve your campus");
        }

        vendorRepository.findByUserId(userId).ifPresent(ownVendor -> {
            if (ownVendor.getId().equals(vendor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot place an order at your own store");
            }
        });

        LocalDateTime scheduledPickupAt = request.scheduledPickupAt();
        OrderType orderType = OrderType.IMMEDIATE;

        if (scheduledPickupAt != null) {
            LocalTime windowStart = LocalTime.parse(schedulingWindowStart);
            LocalTime windowEnd   = LocalTime.parse(schedulingWindowEnd);
            LocalTime pickupTime  = scheduledPickupAt.toLocalTime();

            if (pickupTime.isBefore(windowStart) || pickupTime.isAfter(windowEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Scheduled pickup must be between " + schedulingWindowStart + " and " + schedulingWindowEnd);
            }
            if (scheduledPickupAt.isBefore(LocalDateTime.now().plusMinutes(minLeadMinutes))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Scheduled pickup must be at least " + minLeadMinutes + " minutes from now");
            }
            orderType = OrderType.SCHEDULED;
        }

        List<OrderItem> orderItems = request.items().stream().map(itemReq -> {
            MenuItem menuItem = menuItemRepository.findById(itemReq.menuItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found: " + itemReq.menuItemId()));

            if (!menuItem.getVendor().getId().equals(vendor.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item does not belong to this vendor");
            }

            if (!menuItem.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Item is not available: " + menuItem.getName());
            }

            MenuVariant variant = null;
            BigDecimal unitPrice = menuItem.getPrice();
            String variantLabel = null;

            if (itemReq.variantId() != null) {
                variant = menuVariantRepository.findById(itemReq.variantId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variant not found: " + itemReq.variantId()));
                unitPrice = variant.getPrice();
                variantLabel = variant.getLabel();
            }

            return OrderItem.builder()
                    .menuItem(menuItem)
                    .variant(variant)
                    .variantLabel(variantLabel)
                    .quantity(itemReq.quantity())
                    .unitPrice(unitPrice)
                    .build();
        }).toList();

        BigDecimal subtotal    = orderItems.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgst        = vendor.isGstRegistered() ? subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sgst        = vendor.isGstRegistered() ? subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal taxAmount   = cgst.add(sgst);
        BigDecimal platformFee = subtotal.multiply(PLATFORM_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(platformFee);
        long amountPaise       = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        // Persist the order first so JPA assigns the UUID via @GeneratedValue.
        // If Razorpay fails after save, @Transactional rolls the insert back automatically.
        Order order = Order.builder()
                .user(user)
                .vendor(vendor)
                .orderType(orderType)
                .scheduledPickupAt(scheduledPickupAt)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal)
                .cgst(cgst)
                .sgst(sgst)
                .igst(BigDecimal.ZERO)
                .taxAmount(taxAmount)
                .platformFee(platformFee)
                .totalServiceFee(platformFee)
                .totalAmount(totalAmount)
                .estimatedReadyAt(LocalDateTime.now().plusMinutes(vendor.getPrepTime()))
                .build();

        orderRepository.save(order);
        orderItems.forEach(item -> item.setOrder(order));
        orderItemRepository.saveAll(orderItems);

        String razorpayOrderId;
        try {
            razorpayOrderId = razorpayService.createOrder(amountPaise, order.getId().toString());
        } catch (RazorpayException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment service unavailable. Please try again.");
        }

        order.setRazorpayOrderId(razorpayOrderId);

        // Ably is NOT published here. Vendor sees the order only after
        // payment.captured webhook confirms the payment.
        return new PlaceOrderResponse(order.getId(), razorpayOrderId, amountPaise, razorpayKeyId);
    }

    @Transactional
    public void confirmPayment(String razorpayOrderId, String razorpayPaymentId) {
        Order order = orderRepository.findByRazorpayOrderIdWithItems(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for razorpay_order_id: " + razorpayOrderId));

        order.setPaymentRef(razorpayPaymentId);
        order.setPaymentStatus(PaymentStatus.PAID);

        if (order.getOrderType() == OrderType.SCHEDULED) {
            order.setStatus(OrderStatus.SCHEDULED);
            orderRepository.save(order);
            // Notify customer only — vendor does not see this until dispatch
            ablyService.publish("order:" + order.getId(), "status", toResponse(order, order.getItems()));
        } else {
            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
            OrderResponse response = toResponse(order, order.getItems());
            ablyService.publish("vendor:" + order.getVendor().getId(), "order", response);
            ablyService.publish("order:" + order.getId(), "status", response);
        }
    }

    @Transactional
    public void dispatchScheduledOrder(Order order) {
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        OrderResponse response = toResponse(order, order.getItems());
        ablyService.publish("vendor:" + order.getVendor().getId(), "order", response);
        ablyService.publish("order:" + order.getId(), "status", response);
    }

    @Transactional
    public void handlePaymentFailed(String razorpayOrderId) {
        orderRepository.findByRazorpayOrderIdWithItems(razorpayOrderId).ifPresent(order -> {
            orderItemRepository.deleteAllByOrderIn(List.of(order));
            orderRepository.delete(order);
        });
    }

    @Transactional
    public void cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            // Payment never captured — delete the draft, no refund needed
            orderItemRepository.deleteAllByOrderIn(List.of(order));
            orderRepository.delete(order);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot be cancelled at this stage");
        }

        try {
            long amountPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
            razorpayService.refund(order.getPaymentRef(), amountPaise);
        } catch (RazorpayException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Refund could not be initiated. Please try again.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        ablyService.publish("order:" + order.getId(), "status", toResponse(order, order.getItems()));
        fcmService.sendToUser(order.getUser(), "Order cancelled", "Your order has been cancelled. Full refund initiated.");
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        return toResponse(order, order.getItems());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID userId) {
        return orderRepository.findAllByUserIdWithItems(userId).stream()
                .map(order -> toResponse(order, order.getItems()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getVendorOrders(UUID userId) {
        return orderRepository.findAllByVendorUserIdWithItems(userId).stream()
                .map(order -> toResponse(order, order.getItems()))
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(UUID userId, UUID orderId, OrderStatus newStatus) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getVendor().getId().equals(vendor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to your store");
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.REJECTED) {
            fireRefund(order);
        }
        orderRepository.save(order);

        if (newStatus == OrderStatus.READY) {
            fireVendorTransfer(order);
        }

        OrderResponse response = toResponse(order, order.getItems());
        ablyService.publish("vendor:" + vendor.getId(), "order", response);
        ablyService.publish("order:" + order.getId(), "status", response);
        fcmService.sendToUser(order.getUser(), notificationTitle(newStatus), notificationBody(newStatus, vendor.getName()));

        return response;
    }

    private void fireVendorTransfer(Order order) {
        String linkedAccountId = order.getVendor().getRazorpayLinkedAccountId();
        String paymentRef      = order.getPaymentRef();

        if (linkedAccountId == null || paymentRef == null) {
            log.warn("Skipping transfer for order {} — vendor has no linked account or paymentRef is null",
                    order.getId());
            return;
        }

        long vendorAmountPaise = order.getTotalAmount()
                .subtract(order.getPlatformFee())
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        try {
            razorpayService.transferToVendor(paymentRef,
                    new RazorpayTransferRequest(linkedAccountId, vendorAmountPaise));
        } catch (RazorpayException e) {
            log.error("Razorpay transfer failed for order {} — funds remain in SkipQ account: {}",
                    order.getId(), e.getMessage());
        }
    }

    private void fireRefund(Order order) {
        String paymentRef = order.getPaymentRef();
        if (paymentRef == null) {
            log.warn("Skipping refund for rejected order {} — paymentRef is null", order.getId());
            return;
        }
        long amountPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        try {
            razorpayService.refund(paymentRef, amountPaise);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            log.info("Refund initiated for rejected order {}", order.getId());
        } catch (RazorpayException e) {
            log.error("Refund failed for rejected order {} — manual intervention needed: {}", order.getId(), e.getMessage());
        }
    }

    private String notificationTitle(OrderStatus status) {
        return switch (status) {
            case ACCEPTED  -> "Order accepted!";
            case PREPARING -> "Being prepared";
            case READY     -> "Order ready for pickup!";
            case COMPLETED -> "Order completed";
            case REJECTED  -> "Order rejected";
            default        -> "Order update";
        };
    }

    private String notificationBody(OrderStatus status, String vendorName) {
        return switch (status) {
            case ACCEPTED  -> vendorName + " has accepted your order and will start preparing it shortly.";
            case PREPARING -> vendorName + " is preparing your order now.";
            case READY     -> "Your order at " + vendorName + " is ready! Head over to pick it up.";
            case COMPLETED -> "Thanks for ordering from " + vendorName + ". Enjoy your meal!";
            case REJECTED  -> "Unfortunately " + vendorName + " couldn't accept your order. Full refund initiated.";
            default        -> "Your order status has been updated.";
        };
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
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
        var timeline   = new OrderResponse.Timeline(order.getCreatedAt(), order.getEstimatedReadyAt(), order.getOrderType(), order.getScheduledPickupAt());

        return new OrderResponse(order.getId(), vendorInfo, state, pricing, timeline, itemResponses);
    }
}

package com.skipq.core.order;

import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.order.dto.PlaceOrderRequest;
import com.skipq.core.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public OrderResponse placeOrder(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(user.getUsername(), request);
    }

    @GetMapping("/api/v1/orders")
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal UserDetails user) {
        return orderService.getMyOrders(user.getUsername());
    }

    @GetMapping("/api/v1/orders/{orderId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public OrderResponse getOrder(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId) {
        return orderService.getOrder(user.getUsername(), orderId);
    }

    @GetMapping("/api/v1/vendor/orders")
    @PreAuthorize("hasRole('VENDOR')")
    public List<OrderResponse> getVendorOrders(@AuthenticationPrincipal UserDetails user) {
        return orderService.getVendorOrders(user.getUsername());
    }

    @PatchMapping("/api/v1/vendor/orders/{orderId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public OrderResponse updateStatus(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(user.getUsername(), orderId, request.status());
    }
}

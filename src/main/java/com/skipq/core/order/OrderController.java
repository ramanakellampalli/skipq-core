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
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(userId(userDetails), request);
    }

    @GetMapping("/api/v1/orders")
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return orderService.getMyOrders(userId(userDetails));
    }

    @GetMapping("/api/v1/orders/{orderId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public OrderResponse getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orderId) {
        return orderService.getOrder(userId(userDetails), orderId);
    }

    @GetMapping("/api/v1/vendor/orders")
    @PreAuthorize("hasRole('VENDOR')")
    public List<OrderResponse> getVendorOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return orderService.getVendorOrders(userId(userDetails));
    }

    @PatchMapping("/api/v1/vendor/orders/{orderId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public OrderResponse updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(userId(userDetails), orderId, request.status());
    }

    private UUID userId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}

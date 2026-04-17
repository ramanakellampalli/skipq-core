package com.skipq.core.admin;

import com.skipq.core.admin.dto.AdminStatsResponse;
import com.skipq.core.admin.dto.AdminSyncResponse;
import com.skipq.core.admin.dto.CreateVendorRequest;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.notification.EmailService;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.order.dto.OrderItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.order.dto.OrderStatsProjection;
import com.skipq.core.vendor.VendorRepository;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Transactional
    public void createVendor(CreateVendorRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        String setupToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.ownerName())
                .email(request.email())
                .role(UserRole.VENDOR)
                .setupToken(setupToken)
                .setupTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);

        vendorRepository.save(Vendor.builder()
                .user(user)
                .name(request.vendorName())
                .isOpen(false)
                .prepTime(request.defaultPrepTime())
                .build());

        emailService.sendVendorInvite(request.email(), request.ownerName(), setupToken);

        log.info("Vendor created: {} ({}), invite sent to {}", request.vendorName(), user.getId(), request.email());
    }

    @Transactional(readOnly = true)
    public AdminSyncResponse sync() {
        List<OrderResponse> orders = orderRepository.findAllWithItems().stream()
                .map(o -> {
                    List<OrderItemResponse> items = o.getItems().stream()
                            .map(i -> new OrderItemResponse(
                                    i.getMenuItem().getId(),
                                    i.getMenuItem().getName(),
                                    i.getQuantity(),
                                    i.getUnitPrice(),
                                    i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))
                            )).toList();
                    return new OrderResponse(
                            o.getId(), o.getVendor().getId(), o.getVendor().getName(),
                            o.getStatus(), o.getPaymentStatus(), o.getTotalAmount(),
                            o.getEstimatedReadyAt(), o.getCreatedAt(), items
                    );
                }).toList();

        List<VendorResponse> vendors = vendorRepository.findAll().stream()
                .map(v -> new VendorResponse(v.getId(), v.getName(), v.isOpen(), v.getPrepTime()))
                .toList();

        OrderStatsProjection projection = orderRepository.getTodayStats();
        AdminStatsResponse stats = new AdminStatsResponse(
                projection.getTotalOrders(),
                vendorRepository.countByIsOpenTrue(),
                projection.getInProgress(),
                projection.getRevenue()
        );

        return new AdminSyncResponse(stats, vendors, orders);
    }
}

package com.skipq.core.admin;

import com.skipq.core.admin.dto.*;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.Campus;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.campus.dto.CampusResponse;
import com.skipq.core.common.UserRole;
import com.skipq.core.notification.EmailService;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.order.dto.OrderItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.order.dto.OrderStatsProjection;
import com.skipq.core.vendor.Vendor;
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
    private final CampusRepository campusRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Transactional
    public CampusResponse createCampus(CreateCampusRequest request) {
        Campus campus = Campus.builder()
                .name(request.name())
                .emailDomain(request.emailDomain())
                .build();
        campus = campusRepository.save(campus);
        log.info("Campus created: {} ({})", campus.getName(), campus.getEmailDomain());
        return new CampusResponse(campus.getId(), campus.getName(), campus.getEmailDomain());
    }

    @Transactional
    public void createVendor(CreateVendorRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        Campus campus = campusRepository.findById(request.campusId())
                .orElseThrow(() -> new IllegalArgumentException("Campus not found: " + request.campusId()));

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
                .campus(campus)
                .name(request.vendorName())
                .isOpen(false)
                .prepTime(request.defaultPrepTime())
                .build());

        emailService.sendVendorInvite(request.email(), request.ownerName(), setupToken);

        log.info("Vendor created: {} ({}), campus: {}, invite sent to {}",
                request.vendorName(), user.getId(), campus.getName(), request.email());
    }

    @Transactional(readOnly = true)
    public AdminSyncResponse sync() {
        List<CampusResponse> campuses = campusRepository.findAll().stream()
                .map(c -> new CampusResponse(c.getId(), c.getName(), c.getEmailDomain()))
                .toList();

        List<VendorResponse> vendors = vendorRepository.findAll().stream()
                .map(v -> new VendorResponse(v.getId(), v.getName(), v.isOpen(), v.getPrepTime(),
                        v.getBusinessName(), v.isGstRegistered(), v.getGstin(), v.isKycApproved(),
                        v.getCampus().getId(), v.getCampus().getName()))
                .toList();

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
                    var vendorInfo = new OrderResponse.VendorInfo(o.getVendor().getId(), o.getVendor().getName());
                    var state      = new OrderResponse.OrderState(o.getStatus(), o.getPaymentStatus());
                    var tax        = new OrderResponse.TaxBreakdown(o.getCgst(), o.getSgst(), o.getIgst(), o.getTaxAmount());
                    var fees       = new OrderResponse.Fees(o.getPlatformFee(), o.getPaymentTerminalFee(), o.getTotalServiceFee());
                    var pricing    = new OrderResponse.Pricing(o.getSubtotal(), tax, fees, o.getTotalAmount());
                    var timeline   = new OrderResponse.Timeline(o.getCreatedAt(), o.getEstimatedReadyAt());
                    return new OrderResponse(o.getId(), vendorInfo, state, pricing, timeline, items);
                }).toList();

        OrderStatsProjection projection = orderRepository.getTodayStats();
        AdminStatsResponse stats = new AdminStatsResponse(
                projection.getTotalOrders(),
                vendorRepository.countByIsOpenTrue(),
                projection.getInProgress(),
                projection.getRevenue()
        );

        return new AdminSyncResponse(stats, campuses, vendors, orders);
    }
}

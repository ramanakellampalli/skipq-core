package com.skipq.core.admin;

import com.skipq.core.admin.dto.*;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.Campus;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.campus.dto.CampusResponse;
import com.skipq.core.common.AccountStatus;
import com.skipq.core.common.UserRole;
import com.skipq.core.notification.EmailService;
import com.skipq.core.order.OrderMapper;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.order.dto.OrderStatsProjection;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.support.dto.AdminServiceRequestResponse;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final String DEV_VENDOR_PASSWORD = "Test@1234";

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final CampusRepository campusRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ServiceRequestService serviceRequestService;

    @Value("${otp.bypass:false}")
    private boolean bypass;

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

        if (request.campusId() == null && (request.city() == null || request.city().isBlank())) {
            throw new IllegalArgumentException("City is required for general vendors");
        }

        Campus campus = request.campusId() != null
                ? campusRepository.findById(request.campusId())
                        .orElseThrow(() -> new IllegalArgumentException("Campus not found: " + request.campusId()))
                : null;

        User user;
        Vendor.VendorBuilder vendorBuilder = Vendor.builder()
                .campus(campus)
                .name(request.vendorName())
                .isOpen(false)
                .prepTime(request.defaultPrepTime())
                .city(request.city())
                .phone(request.ownerPhone());

        if (bypass) {
            user = User.builder()
                    .name(request.ownerName())
                    .email(request.email())
                    .phone(request.ownerPhone())
                    .role(UserRole.VENDOR)
                    .passwordHash(passwordEncoder.encode(DEV_VENDOR_PASSWORD))
                    .emailVerified(true)
                    .build();
            userRepository.save(user);

            vendorBuilder
                    .user(user)
                    .businessName(request.vendorName())
                    .pan("ABCDE1234F")
                    .bankAccount("0000000000")
                    .ifsc("SBIN0000000")
                    .gstRegistered(false);

            log.info("[DEV] Vendor created: {} — login: {} / {}", request.vendorName(), request.email(), DEV_VENDOR_PASSWORD);
        } else {
            String setupToken = UUID.randomUUID().toString();
            user = User.builder()
                    .name(request.ownerName())
                    .email(request.email())
                    .phone(request.ownerPhone())
                    .role(UserRole.VENDOR)
                    .setupToken(setupToken)
                    .setupTokenExpiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            userRepository.save(user);

            vendorBuilder.user(user);

            emailService.sendVendorInvite(request.email(), request.ownerName(), setupToken);
            log.info("Vendor created: {} ({}), campus: {}, invite sent to {}",
                    request.vendorName(), user.getId(), campus != null ? campus.getName() : "general", request.email());
        }

        vendorRepository.save(vendorBuilder.build());
    }

    @Transactional(readOnly = true)
    public AdminSyncResponse sync() {
        List<CampusResponse> campuses = campusRepository.findAll().stream()
                .map(c -> new CampusResponse(c.getId(), c.getName(), c.getEmailDomain()))
                .toList();

        List<VendorResponse> vendors = vendorRepository.findAll().stream()
                .map(v -> new VendorResponse(v.getId(), v.getName(), v.isOpen(), v.getPrepTime(),
                        v.getBusinessName(), v.isGstRegistered(), v.getGstin(), v.isKycApproved(),
                        v.getCampus() != null ? v.getCampus().getId() : null,
                        v.getCampus() != null ? v.getCampus().getName() : null,
                        v.getAccountStatus(), v.getSuspensionNote(), v.getLogoUrl(),
                        v.getCity(), v.getPhone()))
                .toList();

        List<OrderResponse> orders = orderRepository.findTodaysOrdersWithItems().stream()
                .map(orderMapper::toResponse).toList();

        OrderStatsProjection projection = orderRepository.getTodayStats();
        AdminStatsResponse stats = new AdminStatsResponse(
                projection.getTotalOrders(),
                vendorRepository.countByIsOpenTrue(),
                projection.getInProgress(),
                projection.getRevenue()
        );

        List<AdminServiceRequestResponse> serviceRequests = serviceRequestService.findAll();

        return new AdminSyncResponse(stats, campuses, vendors, orders, serviceRequests);
    }

    @Transactional
    public void updateVendorStatus(UUID vendorId, UpdateVendorStatusRequest request) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Vendor not found"));
        vendor.setAccountStatus(request.status());
        vendor.setSuspensionNote(request.status() == AccountStatus.SUSPENDED ? request.note() : null);
        vendorRepository.save(vendor);
        log.info("Vendor {} status updated to {} by admin", vendorId, request.status());
    }
}

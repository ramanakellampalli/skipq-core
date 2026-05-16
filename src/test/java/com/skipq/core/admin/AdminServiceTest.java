package com.skipq.core.admin;

import com.skipq.core.admin.dto.UpdateVendorStatusRequest;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.common.AccountStatus;
import com.skipq.core.common.UserRole;
import com.skipq.core.notification.EmailService;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock VendorRepository vendorRepository;
    @Mock UserRepository userRepository;
    @Mock CampusRepository campusRepository;
    @Mock OrderRepository orderRepository;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RazorpayService razorpayService;
    @Mock ServiceRequestService serviceRequestService;

    @InjectMocks AdminService adminService;

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("vendor@campus.edu")
                .role(UserRole.VENDOR)
                .build();

        vendor = Vendor.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Test Stall")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void updateVendorStatus_suspendsSetsNoteAndStatus() {
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        adminService.updateVendorStatus(vendor.getId(),
                new UpdateVendorStatusRequest(AccountStatus.SUSPENDED, "Policy violation"));

        assertThat(vendor.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(vendor.getSuspensionNote()).isEqualTo("Policy violation");
        verify(vendorRepository).save(vendor);
    }

    @Test
    void updateVendorStatus_reinstatesClearsNote() {
        vendor.setAccountStatus(AccountStatus.SUSPENDED);
        vendor.setSuspensionNote("Policy violation");
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        adminService.updateVendorStatus(vendor.getId(),
                new UpdateVendorStatusRequest(AccountStatus.ACTIVE, null));

        assertThat(vendor.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(vendor.getSuspensionNote()).isNull();
        verify(vendorRepository).save(vendor);
    }

    @Test
    void updateVendorStatus_throwsNotFoundWhenVendorMissing() {
        UUID unknownId = UUID.randomUUID();
        when(vendorRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateVendorStatus(unknownId,
                new UpdateVendorStatusRequest(AccountStatus.SUSPENDED, "reason")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor not found");

        verify(vendorRepository, never()).save(any());
    }
}

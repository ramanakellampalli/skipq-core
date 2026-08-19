package com.skipq.core.admin;

import com.skipq.core.admin.dto.CreateVendorRequest;
import com.skipq.core.admin.dto.UpdateVendorStatusRequest;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.Campus;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.common.AccountStatus;
import com.skipq.core.common.UserRole;
import com.skipq.core.notification.EmailService;
import com.skipq.core.order.OrderMapper;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.settlement.VendorLedger;
import com.skipq.core.settlement.VendorLedgerRepository;
import com.skipq.core.subscription.SubscriptionPayment;
import com.skipq.core.subscription.SubscriptionPaymentRepository;
import com.skipq.core.subscription.dto.RecordSubscriptionPaymentRequest;
import com.skipq.core.subscription.dto.UpdateSubscriptionRequest;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.vendor.SubscriptionStatus;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import com.skipq.core.vendor.VendorService;
import com.skipq.core.vendor.dto.SubscriptionInfo;
import com.skipq.core.vendor.dto.VendorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock VendorRepository vendorRepository;
    @Mock UserRepository userRepository;
    @Mock CampusRepository campusRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ServiceRequestService serviceRequestService;
    @Mock VendorLedgerRepository vendorLedgerRepository;
    @Mock SubscriptionPaymentRepository subscriptionPaymentRepository;
    @Mock VendorService vendorService;

    @InjectMocks AdminService adminService;

    private Vendor vendor;

    private static CreateVendorRequest generalRequest(String city) {
        return new CreateVendorRequest(
                "City Cafe", "owner@gmail.com", "Priya", 10,
                null, city, "+91 90000 00001",
                "City Cafe Pvt Ltd", "ABCDE1234F", "123456789012", "SBIN0001234",
                false, null, null);
    }

    private static CreateVendorRequest campusRequest(UUID campusId) {
        return new CreateVendorRequest(
                "Campus Stall", "owner@campus.edu", "Ramana", 15,
                campusId, null, "+91 90000 00001",
                "Campus Stall Pvt Ltd", "XYZPQ5678G", "987654321098", "HDFC0000001",
                true, "29ABCDE1234F1Z5", null);
    }

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

        lenient().when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);
    }

    // --- createVendor ---

    @Test
    void createVendor_generalVendor_bypass_savesVendorWithCityAndPhone() {
        ReflectionTestUtils.setField(adminService, "bypass", true);
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        adminService.createVendor(generalRequest("Bangalore"));

        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(captor.capture());
        assertThat(captor.getValue().getCity()).isEqualTo("Bangalore");
        assertThat(captor.getValue().getPhone()).isEqualTo("+91 90000 00001");
        assertThat(captor.getValue().getCampus()).isNull();
    }

    @Test
    void createVendor_bypass_savesKycFieldsAndAutoApprovesKyc() {
        ReflectionTestUtils.setField(adminService, "bypass", true);
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        adminService.createVendor(generalRequest("Bangalore"));

        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(captor.capture());
        Vendor saved = captor.getValue();
        assertThat(saved.getBusinessName()).isEqualTo("City Cafe Pvt Ltd");
        assertThat(saved.getPan()).isEqualTo("ABCDE1234F");
        assertThat(saved.getBankAccount()).isEqualTo("123456789012");
        assertThat(saved.getIfsc()).isEqualTo("SBIN0001234");
        assertThat(saved.isGstRegistered()).isFalse();
        assertThat(saved.getGstin()).isNull();
        assertThat(saved.isKycApproved()).isTrue();
    }

    @Test
    void createVendor_gstRegistered_savesGstin() {
        ReflectionTestUtils.setField(adminService, "bypass", true);
        UUID campusId = UUID.randomUUID();
        Campus campus = Campus.builder().id(campusId).name("Test Campus").emailDomain("campus.edu").build();
        when(userRepository.existsByEmail("owner@campus.edu")).thenReturn(false);
        when(campusRepository.findById(campusId)).thenReturn(Optional.of(campus));
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        adminService.createVendor(campusRequest(campusId));

        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(captor.capture());
        assertThat(captor.getValue().isGstRegistered()).isTrue();
        assertThat(captor.getValue().getGstin()).isEqualTo("29ABCDE1234F1Z5");
    }

    @Test
    void createVendor_nonBypass_savesKycFieldsAndSendsInvite() {
        ReflectionTestUtils.setField(adminService, "bypass", false);
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(false);

        adminService.createVendor(generalRequest("Bangalore"));

        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(captor.capture());
        Vendor saved = captor.getValue();
        assertThat(saved.getBusinessName()).isEqualTo("City Cafe Pvt Ltd");
        assertThat(saved.getPan()).isEqualTo("ABCDE1234F");
        assertThat(saved.getBankAccount()).isEqualTo("123456789012");
        assertThat(saved.getIfsc()).isEqualTo("SBIN0001234");
        verify(emailService).sendVendorInvite(eq("owner@gmail.com"), eq("Priya"), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createVendor_generalVendor_missingCity_throws(String city) {
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(false);

        assertThatThrownBy(() -> adminService.createVendor(generalRequest(city)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("City is required");

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void createVendor_campusVendor_bypass_savesWithCampus() {
        ReflectionTestUtils.setField(adminService, "bypass", true);
        UUID campusId = UUID.randomUUID();
        Campus campus = Campus.builder().id(campusId).name("Test Campus").emailDomain("campus.edu").build();
        when(userRepository.existsByEmail("owner@campus.edu")).thenReturn(false);
        when(campusRepository.findById(campusId)).thenReturn(Optional.of(campus));
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        adminService.createVendor(campusRequest(campusId));

        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(captor.capture());
        assertThat(captor.getValue().getCampus()).isEqualTo(campus);
    }

    @Test
    void createVendor_campusNotFound_throws() {
        UUID campusId = UUID.randomUUID();
        when(userRepository.existsByEmail("owner@campus.edu")).thenReturn(false);
        when(campusRepository.findById(campusId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createVendor(campusRequest(campusId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campus not found");

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void createVendor_emailAlreadyRegistered_throws() {
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createVendor(generalRequest("Bangalore")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void createVendor_setsOwnerPhoneOnUser() {
        ReflectionTestUtils.setField(adminService, "bypass", true);
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        adminService.createVendor(generalRequest("Bangalore"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPhone()).isEqualTo("+91 90000 00001");
    }

    @Test
    void createVendor_createsVendorLedgerRow() {
        ReflectionTestUtils.setField(adminService, "bypass", true);
        when(userRepository.existsByEmail("owner@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        adminService.createVendor(generalRequest("Bangalore"));

        verify(vendorLedgerRepository).save(any(VendorLedger.class));
    }

    // --- sync ---

    @Test
    void sync_mapsGeneralVendorWithNullCampus() {
        Vendor generalVendor = Vendor.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).email("v@gmail.com").role(UserRole.VENDOR).build())
                .name("City Cafe")
                .campus(null)
                .city("Bangalore")
                .phone("+91 80000 00002")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        com.skipq.core.order.dto.OrderStatsProjection projection =
                mock(com.skipq.core.order.dto.OrderStatsProjection.class);
        when(projection.getTotalOrders()).thenReturn(0L);
        when(projection.getRevenue()).thenReturn(java.math.BigDecimal.ZERO);
        when(projection.getInProgress()).thenReturn(0L);

        var subscription = new SubscriptionInfo(SubscriptionStatus.ACTIVE, java.math.BigDecimal.ZERO, null, null);
        var expectedResponse = new VendorResponse(generalVendor.getId(), "City Cafe", true, 10,
                null, false, null, false, null, null,
                AccountStatus.ACTIVE, null, null, "Bangalore", "+91 80000 00002", subscription);
        when(vendorService.toResponse(generalVendor)).thenReturn(expectedResponse);

        when(campusRepository.findAll()).thenReturn(List.of());
        when(vendorRepository.findAll()).thenReturn(List.of(generalVendor));
        when(orderRepository.findTodaysOrdersWithItems()).thenReturn(List.of());
        when(orderRepository.getTodayStats()).thenReturn(projection);
        when(vendorRepository.countByIsOpenTrue()).thenReturn(0L);
        when(serviceRequestService.findAll()).thenReturn(List.of());

        var response = adminService.sync();

        assertThat(response.vendors()).hasSize(1);
        assertThat(response.vendors().get(0).campusId()).isNull();
        assertThat(response.vendors().get(0).campusName()).isNull();
        assertThat(response.vendors().get(0).city()).isEqualTo("Bangalore");
        assertThat(response.vendors().get(0).phone()).isEqualTo("+91 80000 00002");
    }

    // --- updateVendorStatus ---

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

    // --- updateSubscription ---

    @Test
    void updateSubscription_updatesPrice() {
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        adminService.updateSubscription(vendor.getId(),
                new UpdateSubscriptionRequest(new BigDecimal("999.00")));

        assertThat(vendor.getSubscriptionMonthlyPrice()).isEqualByComparingTo("999.00");
        verify(vendorRepository).save(vendor);
    }

    @Test
    void updateSubscription_throwsNotFoundWhenVendorMissing() {
        UUID unknownId = UUID.randomUUID();
        when(vendorRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateSubscription(unknownId,
                new UpdateSubscriptionRequest(new BigDecimal("999.00"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor not found");
    }

    // --- recordSubscriptionPayment ---

    @Test
    void recordSubscriptionPayment_setsPaidThroughToEndOfMonth() {
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        adminService.recordSubscriptionPayment(vendor.getId(),
                new RecordSubscriptionPaymentRequest(
                        new BigDecimal("999.00"),
                        LocalDate.of(2026, 9, 1),
                        "UPI/123",
                        LocalDate.of(2026, 8, 28),
                        null));

        assertThat(vendor.getSubscriptionPaidThrough()).isEqualTo(LocalDate.of(2026, 9, 30));
        verify(subscriptionPaymentRepository).save(any(SubscriptionPayment.class));
        verify(vendorRepository).save(vendor);
    }

    @Test
    void recordSubscriptionPayment_doesNotMovePaidThroughBackward() {
        vendor.setSubscriptionPaidThrough(LocalDate.of(2026, 12, 31));
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        adminService.recordSubscriptionPayment(vendor.getId(),
                new RecordSubscriptionPaymentRequest(
                        new BigDecimal("999.00"),
                        LocalDate.of(2026, 9, 1),
                        "UPI/backfill",
                        LocalDate.of(2026, 9, 1),
                        "Backfill correction"));

        assertThat(vendor.getSubscriptionPaidThrough()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void recordSubscriptionPayment_advancesPaidThroughWhenNewer() {
        vendor.setSubscriptionPaidThrough(LocalDate.of(2026, 8, 31));
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        adminService.recordSubscriptionPayment(vendor.getId(),
                new RecordSubscriptionPaymentRequest(
                        new BigDecimal("999.00"),
                        LocalDate.of(2026, 9, 1),
                        "UPI/456",
                        LocalDate.of(2026, 9, 1),
                        null));

        assertThat(vendor.getSubscriptionPaidThrough()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void recordSubscriptionPayment_rejectsMidMonthPaidForMonth() {
        assertThatThrownBy(() -> adminService.recordSubscriptionPayment(vendor.getId(),
                new RecordSubscriptionPaymentRequest(
                        new BigDecimal("999.00"),
                        LocalDate.of(2026, 9, 15),
                        null,
                        LocalDate.of(2026, 9, 15),
                        null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("first day");

        verify(vendorRepository, never()).findById(any());
    }

    @Test
    void recordSubscriptionPayment_throwsNotFoundWhenVendorMissing() {
        UUID unknownId = UUID.randomUUID();
        when(vendorRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.recordSubscriptionPayment(unknownId,
                new RecordSubscriptionPaymentRequest(
                        new BigDecimal("999.00"),
                        LocalDate.of(2026, 9, 1),
                        null,
                        LocalDate.of(2026, 9, 1),
                        null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor not found");
    }

    // --- getSubscriptionPayments ---

    @Test
    void getSubscriptionPayments_returnsMappedList() {
        SubscriptionPayment payment = SubscriptionPayment.builder()
                .id(UUID.randomUUID())
                .vendorId(vendor.getId())
                .amount(new BigDecimal("999.00"))
                .paidForMonth(LocalDate.of(2026, 9, 1))
                .paidOn(LocalDate.of(2026, 8, 28))
                .paymentReference("UPI/123")
                .build();
        when(vendorRepository.existsById(vendor.getId())).thenReturn(true);
        when(subscriptionPaymentRepository.findAllByVendorIdOrderByPaidOnDesc(vendor.getId()))
                .thenReturn(List.of(payment));

        var result = adminService.getSubscriptionPayments(vendor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualByComparingTo("999.00");
        assertThat(result.get(0).paymentReference()).isEqualTo("UPI/123");
    }

    @Test
    void getSubscriptionPayments_throwsNotFoundWhenVendorMissing() {
        UUID unknownId = UUID.randomUUID();
        when(vendorRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> adminService.getSubscriptionPayments(unknownId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor not found");
    }

    // --- getVendors ---

    @Test
    void getVendors_noFilter_returnsAll() {
        var sub = new SubscriptionInfo(SubscriptionStatus.ACTIVE, BigDecimal.ZERO, null, null);
        var vr = new VendorResponse(vendor.getId(), "Stall", true, 10, null, false, null, false,
                null, null, AccountStatus.ACTIVE, null, null, null, null, sub);
        when(vendorRepository.findAll()).thenReturn(List.of(vendor));
        when(vendorService.toResponse(vendor)).thenReturn(vr);

        assertThat(adminService.getVendors(null)).hasSize(1);
    }

    @Test
    void getVendors_filterByPastDue_returnsOnlyMatchingVendors() {
        Vendor v2 = Vendor.builder().id(UUID.randomUUID()).build();
        var pastDueSub = new SubscriptionInfo(SubscriptionStatus.PAST_DUE, new BigDecimal("999"), null, null);
        var activeSub  = new SubscriptionInfo(SubscriptionStatus.ACTIVE, BigDecimal.ZERO, null, null);
        var vrPastDue = new VendorResponse(vendor.getId(), "Stall1", true, 10, null, false, null, false,
                null, null, AccountStatus.ACTIVE, null, null, null, null, pastDueSub);
        var vrActive  = new VendorResponse(v2.getId(), "Stall2", true, 10, null, false, null, false,
                null, null, AccountStatus.ACTIVE, null, null, null, null, activeSub);
        when(vendorRepository.findAll()).thenReturn(List.of(vendor, v2));
        when(vendorService.toResponse(vendor)).thenReturn(vrPastDue);
        when(vendorService.toResponse(v2)).thenReturn(vrActive);

        var result = adminService.getVendors("PAST_DUE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).subscription().status()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

}

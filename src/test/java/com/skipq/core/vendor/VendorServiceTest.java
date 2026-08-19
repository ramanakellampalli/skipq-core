package com.skipq.core.vendor;

import com.skipq.core.campus.Campus;
import com.skipq.core.common.AccountStatus;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuItemService;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.order.OrderItemRepository;
import com.skipq.core.order.OrderMapper;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.settlement.PayoutStatus;
import com.skipq.core.settlement.VendorLedger;
import com.skipq.core.settlement.VendorLedgerRepository;
import com.skipq.core.settlement.VendorPayout;
import com.skipq.core.settlement.VendorPayoutRepository;
import com.skipq.core.settlement.dto.VendorPayoutSummary;
import com.skipq.core.subscription.SubscriptionPaymentRepository;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.vendor.SubscriptionStatus;
import com.skipq.core.vendor.dto.VendorDashboardResponse;
import com.skipq.core.vendor.dto.VendorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock VendorRepository vendorRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock MenuItemRepository menuItemRepository;
    @Mock MenuItemService menuItemService;
    @Mock UserRepository userRepository;
    @Mock ServiceRequestService serviceRequestService;
    @Mock OrderMapper orderMapper;
    @Mock VendorLedgerRepository vendorLedgerRepository;
    @Mock VendorPayoutRepository vendorPayoutRepository;
    @Mock SubscriptionPaymentRepository subscriptionPaymentRepository;

    @InjectMocks VendorService vendorService;

    private Campus campus;

    @BeforeEach
    void setUp() {
        campus = Campus.builder()
                .id(UUID.randomUUID())
                .name("Test Campus")
                .emailDomain("campus.edu")
                .build();
    }

    private Vendor campusVendor(String name, boolean open) {
        return Vendor.builder()
                .id(UUID.randomUUID())
                .campus(campus)
                .name(name)
                .isOpen(open)
                .prepTime(10)
                .accountStatus(AccountStatus.ACTIVE)
                .city(null)
                .phone("+91 90000 00001")
                .build();
    }

    private Vendor generalVendor(String name) {
        return Vendor.builder()
                .id(UUID.randomUUID())
                .campus(null)
                .name(name)
                .isOpen(true)
                .prepTime(15)
                .accountStatus(AccountStatus.ACTIVE)
                .city("Bangalore")
                .phone("+91 90000 00002")
                .build();
    }

    @Test
    void getVendorsByCampus_mergesCampusAndGeneralVendors() {
        Vendor cv = campusVendor("Campus Stall", true);
        Vendor gv = generalVendor("City Cafe");

        when(vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE)).thenReturn(List.of(cv));
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of(gv));

        List<VendorResponse> result = vendorService.getVendorsByCampus(campus);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(VendorResponse::name)).containsExactly("Campus Stall", "City Cafe");
    }

    @Test
    void getVendorsByCampus_noGeneralVendors_returnsCampusOnly() {
        Vendor cv = campusVendor("Campus Stall", true);

        when(vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE)).thenReturn(List.of(cv));
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of());

        List<VendorResponse> result = vendorService.getVendorsByCampus(campus);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Campus Stall");
    }

    @Test
    void getGeneralVendors_returnsNullCampusActiveVendors() {
        Vendor gv = generalVendor("City Cafe");

        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of(gv));

        List<VendorResponse> result = vendorService.getGeneralVendors();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("City Cafe");
        assertThat(result.get(0).campusId()).isNull();
        assertThat(result.get(0).campusName()).isNull();
    }

    @Test
    void getGeneralVendors_empty_returnsEmptyList() {
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(vendorService.getGeneralVendors()).isEmpty();
    }

    @Test
    void toResponse_withCampus_mapsCampusFields() {
        Vendor cv = campusVendor("Campus Stall", true);
        when(vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE)).thenReturn(List.of(cv));
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of());

        VendorResponse response = vendorService.getVendorsByCampus(campus).get(0);

        assertThat(response.campusId()).isEqualTo(campus.getId());
        assertThat(response.campusName()).isEqualTo("Test Campus");
    }

    @Test
    void toResponse_nullCampus_mapsCityAndPhone() {
        Vendor gv = generalVendor("City Cafe");
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of(gv));

        VendorResponse response = vendorService.getGeneralVendors().get(0);

        assertThat(response.campusId()).isNull();
        assertThat(response.campusName()).isNull();
        assertThat(response.city()).isEqualTo("Bangalore");
        assertThat(response.phone()).isEqualTo("+91 90000 00002");
    }

    @Test
    void getById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(vendorRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> vendorService.getById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor not found");
    }

    // ── sync() ───────────────────────────────────────────────

    private Vendor syncVendor() {
        return campusVendor("Sync Stall", true);
    }

    private void stubSyncDependencies(Vendor vendor) {
        when(orderRepository.findAllByVendorUserIdWithItems(any())).thenReturn(List.of());
        when(vendorRepository.findByUserId(any())).thenReturn(Optional.of(vendor));
        when(menuItemRepository.findAllByVendorIdWithVariants(vendor.getId())).thenReturn(List.of());
        when(serviceRequestService.findByUser(any())).thenReturn(List.of());
        when(subscriptionPaymentRepository.findFirstByVendorIdOrderByPaidOnDesc(any())).thenReturn(Optional.empty());
    }

    @Test
    void sync_returnsAvailableBalance_fromVendorLedger() {
        Vendor vendor = syncVendor();
        stubSyncDependencies(vendor);
        VendorLedger ledger = VendorLedger.builder()
                .vendorId(vendor.getId())
                .availableBalance(new BigDecimal("1500.00"))
                .build();
        when(vendorLedgerRepository.findById(vendor.getId())).thenReturn(Optional.of(ledger));
        when(vendorPayoutRepository.findTop10ByVendorId(vendor.getId())).thenReturn(List.of());

        VendorDashboardResponse response = vendorService.sync(UUID.randomUUID());

        assertThat(response.availableBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void sync_returnsZeroBalance_whenNoLedgerRow() {
        Vendor vendor = syncVendor();
        stubSyncDependencies(vendor);
        when(vendorLedgerRepository.findById(vendor.getId())).thenReturn(Optional.empty());
        when(vendorPayoutRepository.findTop10ByVendorId(vendor.getId())).thenReturn(List.of());

        VendorDashboardResponse response = vendorService.sync(UUID.randomUUID());

        assertThat(response.availableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sync_mapsRecentPayouts() {
        Vendor vendor = syncVendor();
        stubSyncDependencies(vendor);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        VendorPayout payout = VendorPayout.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .amount(new BigDecimal("800.00"))
                .settlementStartAt(cutoff.minusDays(7))
                .settlementCutoffAt(cutoff)
                .status(PayoutStatus.SUCCESS)
                .payoutReference("UTR123456")
                .build();
        when(vendorLedgerRepository.findById(vendor.getId())).thenReturn(Optional.empty());
        when(vendorPayoutRepository.findTop10ByVendorId(vendor.getId())).thenReturn(List.of(payout));

        VendorDashboardResponse response = vendorService.sync(UUID.randomUUID());

        assertThat(response.recentPayouts()).hasSize(1);
        VendorPayoutSummary summary = response.recentPayouts().get(0);
        assertThat(summary.amount()).isEqualByComparingTo("800.00");
        assertThat(summary.status()).isEqualTo(PayoutStatus.SUCCESS);
        assertThat(summary.payoutReference()).isEqualTo("UTR123456");
        assertThat(summary.settlementCutoffAt()).isEqualTo(cutoff);
    }

    @Test
    void sync_returnsEmptyPayouts_whenNone() {
        Vendor vendor = syncVendor();
        stubSyncDependencies(vendor);
        when(vendorLedgerRepository.findById(vendor.getId())).thenReturn(Optional.empty());
        when(vendorPayoutRepository.findTop10ByVendorId(vendor.getId())).thenReturn(List.of());

        VendorDashboardResponse response = vendorService.sync(UUID.randomUUID());

        assertThat(response.recentPayouts()).isEmpty();
    }

    // --- computedSubscriptionStatus ---

    @Test
    void computedStatus_storedSuspended_returnsSuspended() {
        Vendor v = Vendor.builder()
                .subscriptionStatus("SUSPENDED")
                .subscriptionMonthlyPrice(new BigDecimal("999.00"))
                .subscriptionPaidThrough(LocalDate.now().plusDays(10))
                .build();

        assertThat(v.computedSubscriptionStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
    }

    @Test
    void computedStatus_paidPlanNoPaidThrough_returnsPastDue() {
        Vendor v = Vendor.builder()
                .subscriptionMonthlyPrice(new BigDecimal("999.00"))
                .subscriptionPaidThrough(null)
                .build();

        assertThat(v.computedSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void computedStatus_paidPlanPaidThroughLastMonth_returnsPastDue() {
        LocalDate lastDayOfLastMonth = LocalDate.now().minusMonths(1)
                .withDayOfMonth(LocalDate.now().minusMonths(1).lengthOfMonth());
        Vendor v = Vendor.builder()
                .subscriptionMonthlyPrice(new BigDecimal("999.00"))
                .subscriptionPaidThrough(lastDayOfLastMonth)
                .build();

        assertThat(v.computedSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void computedStatus_paidPlanPaidThroughThisMonth_returnsActive() {
        LocalDate endOfThisMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        Vendor v = Vendor.builder()
                .subscriptionMonthlyPrice(new BigDecimal("999.00"))
                .subscriptionPaidThrough(endOfThisMonth)
                .build();

        assertThat(v.computedSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void computedStatus_freePlan_alwaysReturnsActive() {
        Vendor v = Vendor.builder()
                .subscriptionMonthlyPrice(BigDecimal.ZERO)
                .subscriptionPaidThrough(null)
                .build();

        assertThat(v.computedSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}

package com.skipq.core.settlement;

import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementJobTest {

    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock VendorPayoutRepository vendorPayoutRepository;
    @Mock VendorRepository vendorRepository;

    @InjectMocks SettlementJob settlementJob;

    private static List<Object[]> rows(Object[]... entries) {
        List<Object[]> list = new ArrayList<>();
        Collections.addAll(list, entries);
        return list;
    }

    @Test
    void runDailySettlement_createsPendingPayoutForVendorWithBalance() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = Vendor.builder().id(vendorId).name("Campus Grill").build();
        when(ledgerEntryRepository.sumUnsettledByVendorBeforeCutoff(any()))
                .thenReturn(rows(new Object[]{vendorId, new BigDecimal("350.00")}));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        settlementJob.runDailySettlement();

        ArgumentCaptor<VendorPayout> captor = ArgumentCaptor.forClass(VendorPayout.class);
        verify(vendorPayoutRepository).saveAndFlush(captor.capture());
        VendorPayout saved = captor.getValue();
        assertThat(saved.getVendor()).isEqualTo(vendor);
        assertThat(saved.getAmount()).isEqualByComparingTo("350.00");
        assertThat(saved.getStatus()).isEqualTo(PayoutStatus.PENDING);
        assertThat(saved.getSettlementCutoffAt()).isNotNull();
    }

    @Test
    void runDailySettlement_skipsVendorWithZeroBalance() {
        UUID vendorId = UUID.randomUUID();
        when(ledgerEntryRepository.sumUnsettledByVendorBeforeCutoff(any()))
                .thenReturn(rows(new Object[]{vendorId, new BigDecimal("0.00")}));

        settlementJob.runDailySettlement();

        verify(vendorPayoutRepository, never()).save(any());
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void runDailySettlement_skipsVendorNotFoundInDb() {
        UUID vendorId = UUID.randomUUID();
        when(ledgerEntryRepository.sumUnsettledByVendorBeforeCutoff(any()))
                .thenReturn(rows(new Object[]{vendorId, new BigDecimal("200.00")}));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        settlementJob.runDailySettlement();

        verify(vendorPayoutRepository, never()).save(any());
    }

    @Test
    void runDailySettlement_noUnsettledEntries_createsNoPayouts() {
        when(ledgerEntryRepository.sumUnsettledByVendorBeforeCutoff(any())).thenReturn(new ArrayList<>());

        settlementJob.runDailySettlement();

        verify(vendorPayoutRepository, never()).save(any());
    }

    @Test
    void runDailySettlement_multipleVendors_createsPayoutForEach() {
        UUID vendorId1 = UUID.randomUUID();
        UUID vendorId2 = UUID.randomUUID();
        Vendor v1 = Vendor.builder().id(vendorId1).name("Stall A").build();
        Vendor v2 = Vendor.builder().id(vendorId2).name("Stall B").build();

        when(ledgerEntryRepository.sumUnsettledByVendorBeforeCutoff(any())).thenReturn(rows(
                new Object[]{vendorId1, new BigDecimal("100.00")},
                new Object[]{vendorId2, new BigDecimal("250.00")}
        ));
        when(vendorRepository.findById(vendorId1)).thenReturn(Optional.of(v1));
        when(vendorRepository.findById(vendorId2)).thenReturn(Optional.of(v2));

        settlementJob.runDailySettlement();

        verify(vendorPayoutRepository, times(2)).saveAndFlush(any());
    }
}

package com.skipq.core.settlement;

import com.skipq.core.settlement.dto.MarkPayoutSuccessRequest;
import com.skipq.core.settlement.dto.VendorPayoutResponse;
import com.skipq.core.vendor.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutControllerTest {

    @Mock VendorPayoutRepository vendorPayoutRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks PayoutController payoutController;

    private Vendor vendor;
    private VendorPayout pendingPayout;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder().id(UUID.randomUUID()).name("Campus Grill").build();
        pendingPayout = VendorPayout.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .amount(new BigDecimal("500.00"))
                .settlementStartAt(LocalDateTime.now().minusDays(1))
                .settlementCutoffAt(LocalDateTime.now().minusDays(1).withHour(23).withMinute(59))
                .status(PayoutStatus.PENDING)
                .build();
    }

    @Test
    void listPayouts_noFilter_returnsAll() {
        when(vendorPayoutRepository.findAllWithVendor()).thenReturn(List.of(pendingPayout));

        List<VendorPayoutResponse> result = payoutController.listPayouts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).vendorName()).isEqualTo("Campus Grill");
        assertThat(result.get(0).status()).isEqualTo(PayoutStatus.PENDING);
    }

    @Test
    void listPayouts_withStatusFilter_usesFilteredQuery() {
        when(vendorPayoutRepository.findByStatusWithVendor(PayoutStatus.PENDING))
                .thenReturn(List.of(pendingPayout));

        List<VendorPayoutResponse> result = payoutController.listPayouts(PayoutStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(vendorPayoutRepository).findByStatusWithVendor(PayoutStatus.PENDING);
        verify(vendorPayoutRepository, never()).findAllWithVendor();
    }

    @Test
    void markSuccess_updateStatusAndMarksEntriesSettled() {
        when(vendorPayoutRepository.findById(pendingPayout.getId()))
                .thenReturn(Optional.of(pendingPayout));

        VendorPayoutResponse result = payoutController.markSuccess(
                pendingPayout.getId(),
                new MarkPayoutSuccessRequest("UPI123456", "paid via gpay"));

        assertThat(result.status()).isEqualTo(PayoutStatus.SUCCESS);
        assertThat(result.payoutReference()).isEqualTo("UPI123456");
        verify(vendorPayoutRepository).save(pendingPayout);
        verify(ledgerEntryRepository).markSettled(eq(pendingPayout.getId()));
    }

    @Test
    void markSuccess_alreadySuccess_throwsConflict() {
        pendingPayout.setStatus(PayoutStatus.SUCCESS);
        when(vendorPayoutRepository.findById(pendingPayout.getId()))
                .thenReturn(Optional.of(pendingPayout));

        assertThatThrownBy(() -> payoutController.markSuccess(
                pendingPayout.getId(),
                new MarkPayoutSuccessRequest("ref", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void markFailed_updatesPayout_entriesRemainUnsettled() {
        when(vendorPayoutRepository.findById(pendingPayout.getId()))
                .thenReturn(Optional.of(pendingPayout));

        VendorPayoutResponse result = payoutController.markFailed(pendingPayout.getId(), "bank rejected");

        assertThat(result.status()).isEqualTo(PayoutStatus.FAILED);
        verify(vendorPayoutRepository).save(pendingPayout);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void markFailed_notFound_throws404() {
        when(vendorPayoutRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payoutController.markFailed(UUID.randomUUID(), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}

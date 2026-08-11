package com.skipq.core.settlement;

import com.skipq.core.common.LedgerEntryType;
import com.skipq.core.order.Order;
import com.skipq.core.vendor.Vendor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock VendorLedgerRepository vendorLedgerRepository;

    @InjectMocks LedgerService ledgerService;

    private Order buildOrder(UUID vendorId) {
        Vendor vendor = Vendor.builder().id(vendorId).name("Test Stall").build();
        return Order.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .totalAmount(new BigDecimal("103.00"))
                .platformFee(new BigDecimal("3.00"))
                .subtotal(new BigDecimal("100.00"))
                .cgst(BigDecimal.ZERO).sgst(BigDecimal.ZERO).igst(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalServiceFee(new BigDecimal("3.00"))
                .build();
    }

    @Test
    void creditVendor_savesLedgerEntryAndIncrementsBalance() {
        UUID vendorId = UUID.randomUUID();
        Order order = buildOrder(vendorId);
        when(ledgerEntryRepository.existsByOrderIdAndType(order.getId(), LedgerEntryType.CREDIT))
                .thenReturn(false);

        ledgerService.creditVendor(order);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        LedgerEntry saved = captor.getValue();
        assertThat(saved.getVendorId()).isEqualTo(vendorId);
        assertThat(saved.getOrderId()).isEqualTo(order.getId());
        assertThat(saved.getType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00"); // 103 - 3
        assertThat(saved.isSettled()).isFalse();

        verify(vendorLedgerRepository).upsertBalance(eq(vendorId), eq(new BigDecimal("100.00")));
    }

    @Test
    void creditVendor_idempotent_skipsIfAlreadyCredited() {
        UUID vendorId = UUID.randomUUID();
        Order order = buildOrder(vendorId);
        when(ledgerEntryRepository.existsByOrderIdAndType(order.getId(), LedgerEntryType.CREDIT))
                .thenReturn(true);

        ledgerService.creditVendor(order);

        verify(ledgerEntryRepository, never()).save(any());
        verify(vendorLedgerRepository, never()).upsertBalance(any(), any());
    }

    @Test
    void creditVendor_mdcClearedAfterException() {
        UUID vendorId = UUID.randomUUID();
        Order order = buildOrder(vendorId);
        when(ledgerEntryRepository.existsByOrderIdAndType(order.getId(), LedgerEntryType.CREDIT))
                .thenReturn(false);
        when(ledgerEntryRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> ledgerService.creditVendor(order))
                .isInstanceOf(RuntimeException.class);

        assertThat(MDC.get("event")).isNull();
        assertThat(MDC.get("orderId")).isNull();
        assertThat(MDC.get("vendorId")).isNull();
    }

    @Test
    void creditVendor_vendorShareExcludesPlatformFee() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = Vendor.builder().id(vendorId).name("Stall").build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .totalAmount(new BigDecimal("206.00"))
                .platformFee(new BigDecimal("6.00"))
                .subtotal(new BigDecimal("200.00"))
                .cgst(BigDecimal.ZERO).sgst(BigDecimal.ZERO).igst(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalServiceFee(new BigDecimal("6.00"))
                .build();
        when(ledgerEntryRepository.existsByOrderIdAndType(order.getId(), LedgerEntryType.CREDIT))
                .thenReturn(false);

        ledgerService.creditVendor(order);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("200.00");
    }
}

package com.skipq.core.settlement;

import com.skipq.core.common.LedgerEntryType;
import com.skipq.core.order.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final VendorLedgerRepository vendorLedgerRepository;

    @Transactional
    public void creditVendor(Order order) {
        UUID vendorId = order.getVendor().getId();
        UUID orderId  = order.getId();

        if (ledgerEntryRepository.existsByOrderIdAndType(orderId, LedgerEntryType.CREDIT)) {
            log.warn("Duplicate CREDIT attempted for order {} — skipping", orderId);
            return;
        }

        BigDecimal vendorShare = order.getTotalAmount().subtract(order.getPlatformFee());

        ledgerEntryRepository.save(LedgerEntry.builder()
                .vendorId(vendorId)
                .orderId(orderId)
                .amount(vendorShare)
                .type(LedgerEntryType.CREDIT)
                .build());

        vendorLedgerRepository.upsertBalance(vendorId, vendorShare);

        log.info("Ledger credit: order={} vendor={} amount={}", orderId, vendorId, vendorShare);
    }
}

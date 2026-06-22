package com.skipq.core.settlement;

import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementJob {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final VendorPayoutRepository vendorPayoutRepository;
    private final VendorRepository vendorRepository;

    @Scheduled(cron = "0 0 7 * * *")
    @SchedulerLock(name = "daily_settlement_job", lockAtMostFor = "PT10M")
    @Transactional
    public void runDailySettlement() {
        LocalDateTime cutoff        = LocalDate.now().minusDays(1).atTime(23, 59, 59);
        LocalDateTime settlementStart = LocalDate.now().minusDays(1).atStartOfDay();

        log.info("SettlementJob: running for cutoff={}", cutoff);

        List<Object[]> vendorTotals = ledgerEntryRepository.sumUnsettledByVendorBeforeCutoff(cutoff);

        int created = 0;
        for (Object[] row : vendorTotals) {
            UUID vendorId = (UUID) row[0];
            BigDecimal amount = (BigDecimal) row[1];

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("SettlementJob: vendor {} has zero/negative net — skipping", vendorId);
                continue;
            }

            Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
            if (vendor == null) {
                log.error("SettlementJob: vendor {} not found — skipping", vendorId);
                continue;
            }

            VendorPayout payout = VendorPayout.builder()
                    .vendor(vendor)
                    .amount(amount)
                    .settlementStartAt(settlementStart)
                    .settlementCutoffAt(cutoff)
                    .status(PayoutStatus.PENDING)
                    .build();

            vendorPayoutRepository.save(payout);

            // Reserve entries immediately so a re-run cannot count them again.
            // Entries remain settled=false until admin confirms the bank transfer.
            ledgerEntryRepository.reserveForPayout(vendorId, payout.getId(), cutoff);

            created++;
            log.info("SettlementJob: created payout {} vendor={} amount={}", payout.getId(), vendor.getName(), amount);
        }

        log.info("SettlementJob: completed — {} payout records created", created);
    }
}

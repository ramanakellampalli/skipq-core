package com.skipq.core.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCleanupJob {

    private final OrderRepository orderRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int deleted = orderRepository.deleteStaleAwaitingPaymentOrders(cutoff);
        log.info("Cleanup: deleted {} stale AWAITING_PAYMENT orders older than 24h", deleted);
    }
}

package com.skipq.core.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "jobs.scheduled-dispatch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ScheduledOrderDispatcher {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(cron = "${jobs.scheduled-dispatch.cron:0 * 9-16 * * *}")
    @SchedulerLock(name = "scheduled_order_dispatch", lockAtMostFor = "PT55S")
    public void dispatchDueOrders() {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(15);
        List<Order> due = orderRepository.findDueScheduledOrders(cutoff);
        if (due.isEmpty()) return;
        log.info("Dispatching {} scheduled order(s)", due.size());
        due.forEach(orderService::dispatchScheduledOrder);
    }
}

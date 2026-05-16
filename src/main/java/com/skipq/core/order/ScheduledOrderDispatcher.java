package com.skipq.core.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledOrderDispatcher {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 5_000)
    @SchedulerLock(name = "scheduled_order_dispatch", lockAtMostFor = "PT10S", lockAtLeastFor = "PT4S")
    public void dispatchDueOrders() {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(15);
        List<Order> due = orderRepository.findDueScheduledOrders(cutoff);
        if (due.isEmpty()) return;
        log.info("Dispatching {} scheduled order(s)", due.size());
        due.forEach(orderService::dispatchScheduledOrder);
    }
}

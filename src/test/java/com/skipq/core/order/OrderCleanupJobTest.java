package com.skipq.core.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCleanupJobTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderCleanupJob orderCleanupJob;

    @Test
    void cleanupStaleOrders_deletesOrdersOlderThan24Hours() {
        when(orderRepository.deleteStaleAwaitingPaymentOrders(argThat(cutoff ->
                cutoff.isBefore(LocalDateTime.now().minusHours(23)) &&
                cutoff.isAfter(LocalDateTime.now().minusHours(25))
        ))).thenReturn(3);

        orderCleanupJob.cleanupStaleOrders();

        verify(orderRepository).deleteStaleAwaitingPaymentOrders(argThat(cutoff ->
                cutoff.isBefore(LocalDateTime.now().minusHours(23)) &&
                cutoff.isAfter(LocalDateTime.now().minusHours(25))
        ));
    }

    @Test
    void cleanupStaleOrders_noStaleOrders_logsZero() {
        when(orderRepository.deleteStaleAwaitingPaymentOrders(argThat(cutoff ->
                cutoff.isBefore(LocalDateTime.now().minusHours(23))
        ))).thenReturn(0);

        orderCleanupJob.cleanupStaleOrders();

        verify(orderRepository).deleteStaleAwaitingPaymentOrders(argThat(cutoff ->
                cutoff.isBefore(LocalDateTime.now().minusHours(23))
        ));
    }
}

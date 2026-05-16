package com.skipq.core.order;

import com.skipq.core.auth.User;
import com.skipq.core.campus.Campus;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.OrderType;
import com.skipq.core.common.PaymentStatus;
import com.skipq.core.vendor.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledOrderDispatcherTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;

    @InjectMocks ScheduledOrderDispatcher dispatcher;

    private Vendor vendor;
    private User user;

    @BeforeEach
    void setUp() {
        Campus campus = Campus.builder().id(UUID.randomUUID()).name("Test Campus").emailDomain("test.edu").build();
        user   = User.builder().id(UUID.randomUUID()).name("Student").email("s@test.edu").campus(campus).build();
        vendor = Vendor.builder().id(UUID.randomUUID()).name("Test Stall").isOpen(true).prepTime(15)
                       .gstRegistered(false).campus(campus).build();
    }

    private Order scheduledOrder(LocalDateTime pickupAt) {
        return Order.builder()
                .id(UUID.randomUUID())
                .user(user)
                .vendor(vendor)
                .orderType(OrderType.SCHEDULED)
                .scheduledPickupAt(pickupAt)
                .status(OrderStatus.SCHEDULED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("pay_sched123")
                .subtotal(new BigDecimal("100.00"))
                .cgst(BigDecimal.ZERO)
                .sgst(BigDecimal.ZERO)
                .igst(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .platformFee(new BigDecimal("3.00"))
                .totalServiceFee(new BigDecimal("3.00"))
                .totalAmount(new BigDecimal("103.00"))
                .estimatedReadyAt(LocalDateTime.now().plusMinutes(15))
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void dispatchDueOrders_whenOrdersDue_dispatchesEach() {
        Order o1 = scheduledOrder(LocalDateTime.now().plusMinutes(10));
        Order o2 = scheduledOrder(LocalDateTime.now().plusMinutes(12));
        when(orderRepository.findDueScheduledOrders(any())).thenReturn(List.of(o1, o2));

        dispatcher.dispatchDueOrders();

        verify(orderService).dispatchScheduledOrder(o1);
        verify(orderService).dispatchScheduledOrder(o2);
    }

    @Test
    void dispatchDueOrders_whenNoneDue_doesNothing() {
        when(orderRepository.findDueScheduledOrders(any())).thenReturn(List.of());

        dispatcher.dispatchDueOrders();

        verify(orderService, never()).dispatchScheduledOrder(any());
    }
}

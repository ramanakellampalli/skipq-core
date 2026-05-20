package com.skipq.core.order;

import com.skipq.core.common.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderTransitionPolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING,   EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED),
            OrderStatus.ACCEPTED,  EnumSet.of(OrderStatus.PREPARING, OrderStatus.REJECTED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY, OrderStatus.REJECTED),
            OrderStatus.READY,     EnumSet.of(OrderStatus.COMPLETED)
    );

    public void validate(OrderStatus current, OrderStatus next) {
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot transition order from " + current + " to " + next);
        }
    }
}

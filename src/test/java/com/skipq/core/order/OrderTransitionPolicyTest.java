package com.skipq.core.order;

import com.skipq.core.common.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionPolicyTest {

    private final OrderTransitionPolicy policy = new OrderTransitionPolicy();

    @ParameterizedTest
    @CsvSource({
        "PENDING,   ACCEPTED",
        "PENDING,   REJECTED",
        "ACCEPTED,  PREPARING",
        "ACCEPTED,  REJECTED",
        "PREPARING, READY",
        "PREPARING, REJECTED",
        "READY,     COMPLETED"
    })
    void validate_allowedTransitions_doNotThrow(OrderStatus from, OrderStatus to) {
        assertThatCode(() -> policy.validate(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "PENDING,   PREPARING",
        "PENDING,   READY",
        "PENDING,   COMPLETED",
        "ACCEPTED,  PENDING",
        "ACCEPTED,  COMPLETED",
        "PREPARING, PENDING",
        "PREPARING, ACCEPTED",
        "READY,     ACCEPTED",
        "READY,     REJECTED",
        "COMPLETED, ACCEPTED",
        "REJECTED,  ACCEPTED",
        "CANCELLED, ACCEPTED"
    })
    void validate_forbiddenTransitions_throwConflict(OrderStatus from, OrderStatus to) {
        assertThatThrownBy(() -> policy.validate(from, to))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThatCode(() -> {
                        assert rse.getStatusCode().value() == HttpStatus.CONFLICT.value();
                    }).doesNotThrowAnyException();
                });
    }

    @Test
    void validate_fromScheduled_throwsConflict() {
        assertThatThrownBy(() -> policy.validate(OrderStatus.SCHEDULED, OrderStatus.ACCEPTED))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validate_conflictMessageContainsStatuses() {
        assertThatThrownBy(() -> policy.validate(OrderStatus.READY, OrderStatus.PENDING))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("READY")
                .hasMessageContaining("PENDING");
    }
}

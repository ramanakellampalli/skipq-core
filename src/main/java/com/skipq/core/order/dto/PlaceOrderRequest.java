package com.skipq.core.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull UUID vendorId,
        @NotEmpty @Valid List<OrderItemRequest> items
) {}

package com.skipq.core.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID menuItemId,
        @Min(1) int quantity
) {}

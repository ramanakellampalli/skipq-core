package com.skipq.core.menu.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        @DecimalMin("0.01") BigDecimal price,
        Boolean isAvailable
) {}

package com.skipq.core.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateMenuVariantRequest(
        String label,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        int displayOrder
) {}

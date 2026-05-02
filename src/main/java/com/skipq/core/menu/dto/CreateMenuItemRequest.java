package com.skipq.core.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateMenuItemRequest(
        @NotBlank String name,
        String description,
        boolean isVeg,
        String category,
        int displayOrder,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Valid List<CreateMenuVariantRequest> variants
) {}

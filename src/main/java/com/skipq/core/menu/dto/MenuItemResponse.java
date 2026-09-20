package com.skipq.core.menu.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String category,
        String name,
        String description,
        boolean isVeg,
        boolean isAvailable,
        int displayOrder,
        BigDecimal price,
        List<MenuVariantResponse> variants,
        BigDecimal discountedPrice,   // null when no active discount
        String discountLabel          // e.g. "10% off"; null when no active discount
) {}

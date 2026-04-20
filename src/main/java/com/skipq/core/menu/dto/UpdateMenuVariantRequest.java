package com.skipq.core.menu.dto;

import java.math.BigDecimal;

public record UpdateMenuVariantRequest(
        String label,
        BigDecimal price,
        Boolean isAvailable,
        Integer displayOrder
) {}

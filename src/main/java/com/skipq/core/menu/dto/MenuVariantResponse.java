package com.skipq.core.menu.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuVariantResponse(
        UUID id,
        String label,
        BigDecimal price,
        boolean isAvailable
) {}

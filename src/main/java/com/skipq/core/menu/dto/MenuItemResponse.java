package com.skipq.core.menu.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        BigDecimal price,
        boolean isAvailable
) {}

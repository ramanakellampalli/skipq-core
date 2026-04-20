package com.skipq.core.menu.dto;

import java.util.UUID;

public record UpdateMenuItemRequest(
        String name,
        String description,
        Boolean isVeg,
        Boolean isAvailable,
        UUID categoryId,
        Integer displayOrder
) {}

package com.skipq.core.menu.dto;

import java.util.List;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        boolean isVeg,
        boolean isAvailable,
        int displayOrder,
        List<MenuVariantResponse> variants
) {}

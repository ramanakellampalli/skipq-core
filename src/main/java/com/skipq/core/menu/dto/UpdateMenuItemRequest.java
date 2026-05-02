package com.skipq.core.menu.dto;

import java.util.List;

public record UpdateMenuItemRequest(
        String name,
        String description,
        Boolean isVeg,
        Boolean isAvailable,
        String category,
        Integer displayOrder,
        List<CreateMenuVariantRequest> variants
) {}

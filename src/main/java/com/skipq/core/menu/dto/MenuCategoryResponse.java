package com.skipq.core.menu.dto;

import java.util.List;
import java.util.UUID;

public record MenuCategoryResponse(
        UUID id,
        String name,
        int displayOrder,
        List<MenuItemResponse> items
) {}

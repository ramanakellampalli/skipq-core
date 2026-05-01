package com.skipq.core.menu.dto;

public record UpdateMenuItemRequest(
        String name,
        String description,
        Boolean isVeg,
        Boolean isAvailable,
        String category,
        Integer displayOrder
) {}

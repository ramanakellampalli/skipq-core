package com.skipq.core.menu.dto;

public record UpdateMenuCategoryRequest(
        String name,
        Integer displayOrder
) {}

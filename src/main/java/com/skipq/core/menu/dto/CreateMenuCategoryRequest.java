package com.skipq.core.menu.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMenuCategoryRequest(
        @NotBlank String name,
        int displayOrder
) {}

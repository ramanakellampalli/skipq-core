package com.skipq.core.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateMenuItemRequest(
        @NotBlank String name,
        String description,
        boolean isVeg,
        String category,
        int displayOrder,
        @NotEmpty @Valid List<CreateMenuVariantRequest> variants
) {}

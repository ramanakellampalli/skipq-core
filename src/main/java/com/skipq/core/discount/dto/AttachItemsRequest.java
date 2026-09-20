package com.skipq.core.discount.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AttachItemsRequest(
        @NotEmpty List<UUID> menuItemIds
) {}

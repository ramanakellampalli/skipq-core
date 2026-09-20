package com.skipq.core.discount.dto;

import com.skipq.core.discount.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateDiscountRequest(
        @NotBlank String name,
        @NotNull  DiscountType type,
        @NotNull @Positive BigDecimal value,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}

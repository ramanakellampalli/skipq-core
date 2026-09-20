package com.skipq.core.discount.dto;

import java.time.LocalDateTime;

public record UpdateDiscountRequest(
        String        name,
        Boolean       active,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}

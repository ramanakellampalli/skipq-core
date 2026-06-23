package com.skipq.core.settlement.dto;

import jakarta.validation.constraints.NotBlank;

public record MarkPayoutSuccessRequest(
        @NotBlank String payoutReference,
        String adminNote
) {}

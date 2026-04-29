package com.skipq.core.common;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithMessage() {
        ErrorResponse response = handler.handleNotFound(new NoSuchElementException("No account found"));

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("No account found");
    }
}

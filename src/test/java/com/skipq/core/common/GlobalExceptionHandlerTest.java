package com.skipq.core.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

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

    @Test
    void handleResponseStatus_returns403WithSuspensionNote() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Repeated order cancellations.");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Repeated order cancellations.");
    }
}

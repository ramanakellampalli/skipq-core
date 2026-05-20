package com.skipq.core.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
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
    void handleValidation_includesFieldNameInMessage() {
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "placeOrderRequest");
        bindingResult.rejectValue("vendorId", "NotNull", "must not be null");

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ErrorResponse response = handler.handleValidation(ex);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("vendorId: must not be null");
    }

    @Test
    void handleValidation_multipleErrors_joinsWithComma() {
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "placeOrderRequest");
        bindingResult.rejectValue("vendorId", "NotNull", "must not be null");
        bindingResult.rejectValue("items", "NotEmpty", "must not be empty");

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ErrorResponse response = handler.handleValidation(ex);

        assertThat(response.message()).contains("vendorId: must not be null");
        assertThat(response.message()).contains("items: must not be empty");
    }

    @Test
    void handleResponseStatus_returns403WithSuspensionNote() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Repeated order cancellations.");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Repeated order cancellations.");
    }
}

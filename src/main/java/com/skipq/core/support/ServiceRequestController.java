package com.skipq.core.support;

import com.skipq.core.support.dto.CreateServiceRequestRequest;
import com.skipq.core.support.dto.ServiceRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestResponse create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateServiceRequestRequest request) {
        return service.create(UUID.fromString(userDetails.getUsername()), request);
    }
}

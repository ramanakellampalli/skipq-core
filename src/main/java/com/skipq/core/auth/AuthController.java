package com.skipq.core.auth;

import com.skipq.core.auth.dto.AuthResponse;
import com.skipq.core.auth.dto.LoginRequest;
import com.skipq.core.auth.dto.RegisterRequest;
import com.skipq.core.auth.dto.SetupAccountRequest;
import com.skipq.core.auth.dto.SetupPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/setup-account")
    public AuthResponse setupAccount(@Valid @RequestBody SetupAccountRequest request) {
        return authService.setupAccount(request);
    }

    @PostMapping("/setup-password")
    public AuthResponse setupPassword(@Valid @RequestBody SetupPasswordRequest request) {
        return authService.setupPassword(request);
    }
}

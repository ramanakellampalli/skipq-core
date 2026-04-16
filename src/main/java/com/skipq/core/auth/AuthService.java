package com.skipq.core.auth;

import com.skipq.core.auth.dto.AuthResponse;
import com.skipq.core.auth.dto.LoginRequest;
import com.skipq.core.auth.dto.RegisterRequest;
import com.skipq.core.auth.dto.SetupPasswordRequest;
import com.skipq.core.common.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.STUDENT)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return toResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user);
        return toResponse(token, user);
    }

    @Transactional
    public AuthResponse setupPassword(SetupPasswordRequest request) {
        User user = userRepository.findBySetupToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired setup token"));

        if (user.getSetupTokenExpiresAt() == null || LocalDateTime.now().isAfter(user.getSetupTokenExpiresAt())) {
            throw new IllegalArgumentException("Setup token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setSetupToken(null);
        user.setSetupTokenExpiresAt(null);
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return toResponse(jwtToken, user);
    }

    private AuthResponse toResponse(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}

package com.skipq.core.auth;

import com.skipq.core.auth.dto.*;
import com.skipq.core.campus.Campus;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final CampusRepository campusRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RazorpayService razorpayService;
    private final OtpService otpService;

    @Value("${otp.allowed-test-domain:test.skipq.dev}")
    private String testDomain;

    @Transactional
    public OtpSentResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Campus campus = resolveCampus(request.email());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .role(UserRole.STUDENT)
                .campus(campus)
                .build();

        userRepository.save(user);
        otpService.generateAndSend(user);

        return new OtpSentResponse("OTP sent to " + request.email());
    }

    @Transactional
    public OtpSentResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));

        if (user.getRole() == UserRole.STUDENT) {
            otpService.generateAndSend(user);
            return new OtpSentResponse("OTP sent to " + request.email());
        }

        // Vendor / admin — password login (returns null, handled by controller branching)
        return null;
    }

    public AuthResponse loginWithPassword(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toAuthResponse(jwtService.generateToken(user), user);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));

        if (!otpService.verify(user, request.code())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        user.setEmailVerified(true);
        otpService.clear(user);

        return toAuthResponse(jwtService.generateToken(user), user);
    }

    @Transactional
    public AuthResponse setupAccount(SetupAccountRequest request) {
        User user = userRepository.findBySetupToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired setup token"));

        if (user.getSetupTokenExpiresAt() == null || LocalDateTime.now().isAfter(user.getSetupTokenExpiresAt())) {
            throw new IllegalArgumentException("Setup token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setSetupToken(null);
        user.setSetupTokenExpiresAt(null);
        userRepository.save(user);

        Vendor vendor = vendorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user"));

        vendor.setBusinessName(request.businessName());
        vendor.setPan(request.pan());
        vendor.setBankAccount(request.bankAccount());
        vendor.setIfsc(request.ifsc());
        vendor.setGstRegistered(request.gstRegistered());
        vendor.setGstin(request.gstRegistered() ? request.gstin() : null);

        try {
            String linkedAccountId = razorpayService.createLinkedAccount(
                    request.businessName(), request.pan(),
                    request.bankAccount(), request.ifsc()
            );
            vendor.setRazorpayLinkedAccountId(linkedAccountId);
        } catch (Exception e) {
            log.error("Razorpay linked account creation failed for vendor {}: {}", vendor.getId(), e.getMessage());
        }

        vendorRepository.save(vendor);

        return toAuthResponse(jwtService.generateToken(user), user);
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

        return toAuthResponse(jwtService.generateToken(user), user);
    }

    private Campus resolveCampus(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        if (domain.equals(testDomain)) {
            // Test domain: use first campus as a stand-in
            return campusRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No campuses configured"));
        }
        return campusRepository.findByEmailDomain(domain)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Email domain @" + domain + " is not affiliated with any campus"));
    }

    private AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}

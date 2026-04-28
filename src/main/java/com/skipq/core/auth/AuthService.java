package com.skipq.core.auth;

import com.skipq.core.auth.dto.*;
import com.skipq.core.campus.Campus;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.notification.EmailService;
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
    private final EmailService emailService;

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
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.STUDENT)
                .campus(campus)
                .build();

        userRepository.save(user);
        otpService.generateAndSend(user);

        return new OtpSentResponse("OTP sent to " + request.email());
    }

    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.email());
        
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.debug("Login failed - user not found for email: {}", request.email());
                    throw new IllegalArgumentException("User not found");
                });

        log.debug("Found user: {} with role: {}", user.getId(), user.getRole());
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getId().toString(), request.password())
            );
            log.debug("Authentication successful for user: {}", user.getId());
        } catch (Exception e) {
            log.debug("Authentication failed for user: {} - {}", user.getId(), e.getMessage());
            throw e;
        }

        if (user.getRole() == UserRole.STUDENT && !user.isEmailVerified()) {
            log.debug("Student email not verified, sending OTP for user: {}", user.getId());
            otpService.generateAndSend(user);
            throw new IllegalStateException("Email not verified. A new OTP has been sent to " + user.getEmail());
        }

        AuthResponse response = toAuthResponse(jwtService.generateToken(user), user);
        log.debug("Login successful for user: {} with role: {}", user.getId(), user.getRole());
        return response;
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

    @Transactional
    public OtpSentResponse forgotPassword(ForgotPasswordRequest request) {
        if (request.role() == UserRole.VENDOR) {
            vendorRepository.findByUserEmail(request.email()).ifPresent(vendor -> {
                String code = otpService.generateCode();
                vendor.setResetOtp(code);
                vendor.setResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
                vendorRepository.save(vendor);
                emailService.sendOtp(vendor.getUser().getEmail(), vendor.getUser().getName(), code);
            });
        } else {
            userRepository.findByEmail(request.email())
                    .filter(u -> u.getRole() == UserRole.STUDENT)
                    .ifPresent(otpService::generateAndSend);
        }
        return new OtpSentResponse("If an account exists for that email, an OTP has been sent.");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.role() == UserRole.VENDOR) {
            Vendor vendor = vendorRepository.findByUserEmail(request.email())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));

            if (vendor.getResetOtp() == null || vendor.getResetOtpExpiresAt() == null
                    || LocalDateTime.now().isAfter(vendor.getResetOtpExpiresAt())
                    || !vendor.getResetOtp().equals(request.otp())) {
                throw new IllegalArgumentException("Invalid or expired OTP");
            }

            vendor.getUser().setPasswordHash(passwordEncoder.encode(request.newPassword()));
            vendor.setResetOtp(null);
            vendor.setResetOtpExpiresAt(null);
            vendorRepository.save(vendor);
        } else {
            User user = userRepository.findByEmail(request.email())
                    .filter(u -> u.getRole() == UserRole.STUDENT)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP"));

            if (!otpService.verify(user, request.otp())) {
                throw new IllegalArgumentException("Invalid or expired OTP");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            otpService.clear(user);
        }
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

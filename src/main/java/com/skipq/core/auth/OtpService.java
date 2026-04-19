package com.skipq.core.auth;

import com.skipq.core.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${otp.bypass:false}")
    private boolean bypass;

    @Value("${otp.fixed-code:123456}")
    private String fixedCode;

    private static final SecureRandom RANDOM = new SecureRandom();

    public void generateAndSend(User user) {
        String code = bypass ? fixedCode : String.format("%06d", RANDOM.nextInt(1_000_000));

        user.setOtpCode(code);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        if (bypass) {
            log.info("[DEV] OTP for {}: {}", user.getEmail(), code);
        } else {
            emailService.sendOtp(user.getEmail(), user.getName(), code);
        }
    }

    public boolean verify(User user, String code) {
        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null) return false;
        if (LocalDateTime.now().isAfter(user.getOtpExpiresAt())) return false;
        return user.getOtpCode().equals(code);
    }

    public void clear(User user) {
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
    }
}

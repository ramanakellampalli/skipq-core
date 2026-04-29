package com.skipq.core.auth;

import com.skipq.core.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmailService emailService;

    @InjectMocks OtpService otpService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@campus.edu")
                .build();
    }

    // --- generateCode ---

    @Test
    void generateCode_returnsFixedCodeWhenBypass() {
        ReflectionTestUtils.setField(otpService, "bypass", true);
        ReflectionTestUtils.setField(otpService, "fixedCode", "111111");

        assertThat(otpService.generateCode()).isEqualTo("111111");
    }

    @Test
    void generateCode_returnsSixDigitCodeWhenNotBypass() {
        ReflectionTestUtils.setField(otpService, "bypass", false);

        String code = otpService.generateCode();
        assertThat(code).matches("\\d{6}");
    }

    // --- generateAndSend ---

    @Test
    void generateAndSend_sendsEmailWithPurposeWhenNotBypass() {
        ReflectionTestUtils.setField(otpService, "bypass", false);
        ReflectionTestUtils.setField(otpService, "fixedCode", "123456");

        otpService.generateAndSend(user, OtpPurpose.VERIFY_EMAIL);

        assertThat(user.getOtpCode()).isNotBlank();
        assertThat(user.getOtpExpiresAt()).isAfter(LocalDateTime.now());
        verify(userRepository).save(user);
        verify(emailService).sendOtp(eq("user@campus.edu"), eq("Test User"), anyString(), eq(OtpPurpose.VERIFY_EMAIL));
    }

    @Test
    void generateAndSend_logsAndSkipsEmailWhenBypass() {
        ReflectionTestUtils.setField(otpService, "bypass", true);
        ReflectionTestUtils.setField(otpService, "fixedCode", "123456");

        otpService.generateAndSend(user, OtpPurpose.STUDENT_RESET);

        verify(userRepository).save(user);
        verifyNoInteractions(emailService);
    }

    // --- sendEmail ---

    @Test
    void sendEmail_delegatesToEmailServiceWithPurposeWhenNotBypass() {
        ReflectionTestUtils.setField(otpService, "bypass", false);

        otpService.sendEmail("vendor@campus.edu", "Vendor", "654321", OtpPurpose.VENDOR_RESET);

        verify(emailService).sendOtp("vendor@campus.edu", "Vendor", "654321", OtpPurpose.VENDOR_RESET);
    }

    @Test
    void sendEmail_logsAndSkipsEmailWhenBypass() {
        ReflectionTestUtils.setField(otpService, "bypass", true);

        otpService.sendEmail("vendor@campus.edu", "Vendor", "654321", OtpPurpose.VENDOR_RESET);

        verifyNoInteractions(emailService);
    }

    // --- verify ---

    @Test
    void verify_returnsTrueWhenCodeMatchesAndNotExpired() {
        user.setOtpCode("123456");
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        assertThat(otpService.verify(user, "123456")).isTrue();
    }

    @Test
    void verify_returnsFalseWhenExpired() {
        user.setOtpCode("123456");
        user.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThat(otpService.verify(user, "123456")).isFalse();
    }

    @Test
    void verify_returnsFalseWhenCodeNull() {
        assertThat(otpService.verify(user, "123456")).isFalse();
    }

    @Test
    void verify_returnsFalseWhenCodeWrong() {
        user.setOtpCode("123456");
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        assertThat(otpService.verify(user, "000000")).isFalse();
    }

    // --- clear ---

    @Test
    void clear_nullsOtpFieldsAndSaves() {
        user.setOtpCode("123456");
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        otpService.clear(user);

        assertThat(user.getOtpCode()).isNull();
        assertThat(user.getOtpExpiresAt()).isNull();
        verify(userRepository).save(user);
    }
}

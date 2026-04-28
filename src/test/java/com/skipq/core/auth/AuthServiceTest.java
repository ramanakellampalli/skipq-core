package com.skipq.core.auth;

import com.skipq.core.auth.dto.ForgotPasswordRequest;
import com.skipq.core.auth.dto.OtpSentResponse;
import com.skipq.core.auth.dto.ResetPasswordRequest;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock VendorRepository vendorRepository;
    @Mock CampusRepository campusRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock RazorpayService razorpayService;
    @Mock OtpService otpService;

    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@campus.edu")
                .role(UserRole.STUDENT)
                .emailVerified(true)
                .build();
    }

    // --- forgotPassword ---

    @Test
    void forgotPassword_sendsOtpWhenUserExists() {
        when(userRepository.findByEmail("test@campus.edu")).thenReturn(Optional.of(user));

        OtpSentResponse response = authService.forgotPassword(new ForgotPasswordRequest("test@campus.edu"));

        assertThat(response.message()).isNotBlank();
        verify(otpService).generateAndSend(user);
    }

    @Test
    void forgotPassword_doesNotRevealWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@campus.edu")).thenReturn(Optional.empty());

        OtpSentResponse response = authService.forgotPassword(new ForgotPasswordRequest("unknown@campus.edu"));

        assertThat(response.message()).isNotBlank();
        verify(otpService, never()).generateAndSend(any());
    }

    // --- resetPassword ---

    @Test
    void resetPassword_updatesPasswordWhenOtpValid() {
        user.setOtpCode("123456");
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@campus.edu")).thenReturn(Optional.of(user));
        when(otpService.verify(user, "123456")).thenReturn(true);
        when(passwordEncoder.encode("newSecret8!")).thenReturn("hashed");

        authService.resetPassword(new ResetPasswordRequest("test@campus.edu", "123456", "newSecret8!"));

        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        verify(otpService).clear(user);
    }

    @Test
    void resetPassword_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("nobody@campus.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("nobody@campus.edu", "000000", "newSecret8!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(otpService, never()).verify(any(), any());
        verify(otpService, never()).clear(any());
    }

    @Test
    void resetPassword_throwsWhenOtpInvalid() {
        when(userRepository.findByEmail("test@campus.edu")).thenReturn(Optional.of(user));
        when(otpService.verify(user, "000000")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("test@campus.edu", "000000", "newSecret8!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(passwordEncoder, never()).encode(any());
        verify(otpService, never()).clear(any());
    }
}

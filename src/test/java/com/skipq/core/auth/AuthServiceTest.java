package com.skipq.core.auth;

import com.skipq.core.auth.dto.ForgotPasswordRequest;
import com.skipq.core.auth.dto.OtpSentResponse;
import com.skipq.core.auth.dto.ResetPasswordRequest;
import com.skipq.core.campus.CampusRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.notification.EmailService;
import com.skipq.core.vendor.Vendor;
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
    @Mock EmailService emailService;

    @InjectMocks AuthService authService;

    private User studentUser;
    private User vendorUser;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Student User")
                .email("student@campus.edu")
                .role(UserRole.STUDENT)
                .emailVerified(true)
                .build();

        vendorUser = User.builder()
                .id(UUID.randomUUID())
                .name("Vendor User")
                .email("vendor@campus.edu")
                .role(UserRole.VENDOR)
                .emailVerified(true)
                .build();

        vendor = Vendor.builder()
                .id(UUID.randomUUID())
                .user(vendorUser)
                .name("Test Stall")
                .build();
    }

    // --- forgotPassword: customer ---

    @Test
    void forgotPassword_customer_sendsOtpViaOtpService() {
        when(userRepository.findByEmail("student@campus.edu")).thenReturn(Optional.of(studentUser));

        OtpSentResponse response = authService.forgotPassword(new ForgotPasswordRequest("student@campus.edu"));

        assertThat(response.message()).isNotBlank();
        verify(otpService).generateAndSend(studentUser);
        verifyNoInteractions(vendorRepository, emailService);
    }

    @Test
    void forgotPassword_doesNotRevealWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@campus.edu")).thenReturn(Optional.empty());

        OtpSentResponse response = authService.forgotPassword(new ForgotPasswordRequest("unknown@campus.edu"));

        assertThat(response.message()).isNotBlank();
        verify(otpService, never()).generateAndSend(any());
        verifyNoInteractions(vendorRepository, emailService);
    }

    // --- forgotPassword: vendor ---

    @Test
    void forgotPassword_vendor_storesResetOtpOnVendorAndSendsEmail() {
        when(userRepository.findByEmail("vendor@campus.edu")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(otpService.generateCode()).thenReturn("654321");

        authService.forgotPassword(new ForgotPasswordRequest("vendor@campus.edu"));

        assertThat(vendor.getResetOtp()).isEqualTo("654321");
        assertThat(vendor.getResetOtpExpiresAt()).isAfter(LocalDateTime.now());
        verify(vendorRepository).save(vendor);
        verify(emailService).sendOtp("vendor@campus.edu", "Vendor User", "654321");
        verify(otpService, never()).generateAndSend(any());
    }

    // --- resetPassword: customer ---

    @Test
    void resetPassword_customer_updatesPasswordWhenOtpValid() {
        when(userRepository.findByEmail("student@campus.edu")).thenReturn(Optional.of(studentUser));
        when(otpService.verify(studentUser, "123456")).thenReturn(true);
        when(passwordEncoder.encode("newSecret8!")).thenReturn("hashed");

        authService.resetPassword(new ResetPasswordRequest("student@campus.edu", "123456", "newSecret8!"));

        assertThat(studentUser.getPasswordHash()).isEqualTo("hashed");
        verify(otpService).clear(studentUser);
    }

    @Test
    void resetPassword_customer_throwsWhenOtpInvalid() {
        when(userRepository.findByEmail("student@campus.edu")).thenReturn(Optional.of(studentUser));
        when(otpService.verify(studentUser, "000000")).thenReturn(false);

        var req = new ResetPasswordRequest("student@campus.edu", "000000", "newSecret8!");
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(passwordEncoder, never()).encode(any());
        verify(otpService, never()).clear(any());
    }

    @Test
    void resetPassword_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("nobody@campus.edu")).thenReturn(Optional.empty());

        var req = new ResetPasswordRequest("nobody@campus.edu", "000000", "newSecret8!");
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(otpService, never()).verify(any(), any());
        verify(otpService, never()).clear(any());
    }

    // --- resetPassword: vendor ---

    @Test
    void resetPassword_vendor_updatesPasswordWhenOtpValid() {
        vendor.setResetOtp("654321");
        vendor.setResetOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("vendor@campus.edu")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));
        when(passwordEncoder.encode("newSecret8!")).thenReturn("hashed");

        authService.resetPassword(new ResetPasswordRequest("vendor@campus.edu", "654321", "newSecret8!"));

        assertThat(vendorUser.getPasswordHash()).isEqualTo("hashed");
        assertThat(vendor.getResetOtp()).isNull();
        assertThat(vendor.getResetOtpExpiresAt()).isNull();
        verify(vendorRepository).save(vendor);
        verify(otpService, never()).verify(any(), any());
    }

    @Test
    void resetPassword_vendor_throwsWhenOtpExpired() {
        vendor.setResetOtp("654321");
        vendor.setResetOtpExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("vendor@campus.edu")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));

        var req = new ResetPasswordRequest("vendor@campus.edu", "654321", "newSecret8!");
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void resetPassword_vendor_throwsWhenOtpWrong() {
        vendor.setResetOtp("654321");
        vendor.setResetOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("vendor@campus.edu")).thenReturn(Optional.of(vendorUser));
        when(vendorRepository.findByUserId(vendorUser.getId())).thenReturn(Optional.of(vendor));

        var req = new ResetPasswordRequest("vendor@campus.edu", "000000", "newSecret8!");
        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(passwordEncoder, never()).encode(any());
    }
}

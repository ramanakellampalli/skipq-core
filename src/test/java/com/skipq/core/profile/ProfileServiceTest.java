package com.skipq.core.profile;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.R2ImageService;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock UserRepository userRepository;
    @Mock VendorRepository vendorRepository;
    @Mock R2ImageService r2ImageService;

    @InjectMocks ProfileService profileService;

    private static final byte[] VALID_BYTES = new byte[100];
    private static final String JPEG = "image/jpeg";

    private User studentUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setRole(UserRole.STUDENT);
        return u;
    }

    private User vendorUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setRole(UserRole.VENDOR);
        return u;
    }

    @Test
    void uploadAvatar_student_setsAvatarUrl() {
        UUID userId = UUID.randomUUID();
        User user = studentUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(r2ImageService.uploadAvatar(userId, VALID_BYTES, JPEG)).thenReturn("https://cdn/avatars/" + userId);

        String url = profileService.uploadAvatar(userId, VALID_BYTES, JPEG);

        assertThat(url).isEqualTo("https://cdn/avatars/" + userId);
        assertThat(user.getAvatarUrl()).isEqualTo(url);
        verify(userRepository).save(user);
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void uploadAvatar_vendor_setsLogoUrl() {
        UUID userId = UUID.randomUUID();
        User user = vendorUser(userId);
        Vendor vendor = new Vendor();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(r2ImageService.uploadAvatar(userId, VALID_BYTES, JPEG)).thenReturn("https://cdn/avatars/" + userId);

        String url = profileService.uploadAvatar(userId, VALID_BYTES, JPEG);

        assertThat(url).isEqualTo("https://cdn/avatars/" + userId);
        assertThat(vendor.getLogoUrl()).isEqualTo(url);
        verify(vendorRepository).save(vendor);
        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_pngContentType_accepted() {
        UUID userId = UUID.randomUUID();
        User user = studentUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(r2ImageService.uploadAvatar(any(), any(), eq("image/png"))).thenReturn("https://cdn/avatars/" + userId);

        assertThat(profileService.uploadAvatar(userId, VALID_BYTES, "image/png")).isNotNull();
    }

    @Test
    void uploadAvatar_webpContentType_accepted() {
        UUID userId = UUID.randomUUID();
        User user = studentUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(r2ImageService.uploadAvatar(any(), any(), eq("image/webp"))).thenReturn("https://cdn/avatars/" + userId);

        assertThat(profileService.uploadAvatar(userId, VALID_BYTES, "image/webp")).isNotNull();
    }

    @Test
    void uploadAvatar_nullContentType_throws400() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> profileService.uploadAvatar(userId, VALID_BYTES, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(userRepository, r2ImageService);
    }

    @Test
    void uploadAvatar_unsupportedType_throws400() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> profileService.uploadAvatar(userId, VALID_BYTES, "application/pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(userRepository, r2ImageService);
    }

    @Test
    void uploadAvatar_fileTooLarge_throws400() {
        UUID userId = UUID.randomUUID();
        byte[] bigFile = new byte[6 * 1024 * 1024];

        assertThatThrownBy(() -> profileService.uploadAvatar(userId, bigFile, JPEG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(userRepository, r2ImageService);
    }

    @Test
    void uploadAvatar_userNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.uploadAvatar(userId, VALID_BYTES, JPEG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void uploadAvatar_vendorRecordNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        User user = vendorUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(r2ImageService.uploadAvatar(any(), any(), any())).thenReturn("https://cdn/avatars/" + userId);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.uploadAvatar(userId, VALID_BYTES, JPEG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }
}

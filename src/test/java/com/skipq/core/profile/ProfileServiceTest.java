package com.skipq.core.profile;

import com.skipq.core.auth.UserRepository;
import com.skipq.core.config.R2ImageService;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock UserRepository userRepository;
    @Mock VendorRepository vendorRepository;
    @Mock R2ImageService r2ImageService;

    @InjectMocks ProfileService profileService;

    private static final byte[] VALID_BYTES = new byte[100];
    private static final String JPEG = "image/jpeg";

    @Test
    void uploadVendorLogo_uploadsAndUpdates() {
        UUID vendorId = UUID.randomUUID();
        String expectedUrl = "https://cdn/avatars/" + vendorId;
        when(r2ImageService.uploadAvatar(vendorId, VALID_BYTES, JPEG)).thenReturn(expectedUrl);

        String url = profileService.uploadVendorLogo(vendorId, VALID_BYTES, JPEG);

        assertThat(url).isEqualTo(expectedUrl);
        verify(vendorRepository).updateLogoUrl(vendorId, expectedUrl);
        verifyNoInteractions(userRepository);
    }

    @Test
    void uploadAvatar_uploadsAndUpdates() {
        UUID userId = UUID.randomUUID();
        String expectedUrl = "https://cdn/avatars/" + userId;
        when(r2ImageService.uploadAvatar(userId, VALID_BYTES, JPEG)).thenReturn(expectedUrl);

        String url = profileService.uploadAvatar(userId, VALID_BYTES, JPEG);

        assertThat(url).isEqualTo(expectedUrl);
        verify(userRepository).updateAvatarUrl(userId, expectedUrl);
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void uploadVendorLogo_pngAccepted() {
        UUID vendorId = UUID.randomUUID();
        when(r2ImageService.uploadAvatar(any(), any(), eq("image/png"))).thenReturn("https://cdn/avatars/" + vendorId);

        assertThat(profileService.uploadVendorLogo(vendorId, VALID_BYTES, "image/png")).isNotNull();
    }

    @Test
    void uploadVendorLogo_webpAccepted() {
        UUID vendorId = UUID.randomUUID();
        when(r2ImageService.uploadAvatar(any(), any(), eq("image/webp"))).thenReturn("https://cdn/avatars/" + vendorId);

        assertThat(profileService.uploadVendorLogo(vendorId, VALID_BYTES, "image/webp")).isNotNull();
    }

    @Test
    void uploadVendorLogo_nullContentType_throws400() {
        assertThatThrownBy(() -> profileService.uploadVendorLogo(UUID.randomUUID(), VALID_BYTES, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }

    @Test
    void uploadVendorLogo_unsupportedType_throws400() {
        assertThatThrownBy(() -> profileService.uploadVendorLogo(UUID.randomUUID(), VALID_BYTES, "application/pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }

    @Test
    void uploadVendorLogo_fileTooLarge_throws400() {
        byte[] bigFile = new byte[6 * 1024 * 1024];
        assertThatThrownBy(() -> profileService.uploadVendorLogo(UUID.randomUUID(), bigFile, JPEG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }

    @Test
    void uploadAvatar_fileTooLarge_throws400() {
        byte[] bigFile = new byte[6 * 1024 * 1024];
        assertThatThrownBy(() -> profileService.uploadAvatar(UUID.randomUUID(), bigFile, JPEG))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }
}

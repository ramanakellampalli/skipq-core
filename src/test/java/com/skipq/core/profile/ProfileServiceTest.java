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

    @Test
    void upload_vendorType_updatesVendorLogoUrl() {
        UUID id = UUID.randomUUID();
        when(r2ImageService.uploadAvatar(id, VALID_BYTES, JPEG)).thenReturn("https://cdn/" + id);

        String url = profileService.upload(id, VALID_BYTES, JPEG, "VENDOR");

        assertThat(url).isEqualTo("https://cdn/" + id);
        verify(vendorRepository).updateLogoUrl(id, url);
        verifyNoInteractions(userRepository);
    }

    @Test
    void upload_userType_updatesUserAvatarUrl() {
        UUID id = UUID.randomUUID();
        when(r2ImageService.uploadAvatar(id, VALID_BYTES, JPEG)).thenReturn("https://cdn/" + id);

        String url = profileService.upload(id, VALID_BYTES, JPEG, "USER");

        assertThat(url).isEqualTo("https://cdn/" + id);
        verify(userRepository).updateAvatarUrl(id, url);
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void upload_pngAccepted() {
        UUID id = UUID.randomUUID();
        when(r2ImageService.uploadAvatar(any(), any(), eq("image/png"))).thenReturn("https://cdn/" + id);

        assertThat(profileService.upload(id, VALID_BYTES, "image/png", "USER")).isNotNull();
    }

    @Test
    void upload_webpAccepted() {
        UUID id = UUID.randomUUID();
        when(r2ImageService.uploadAvatar(any(), any(), eq("image/webp"))).thenReturn("https://cdn/" + id);

        assertThat(profileService.upload(id, VALID_BYTES, "image/webp", "USER")).isNotNull();
    }

    @Test
    void upload_nullContentType_throws400() {
        assertThatThrownBy(() -> profileService.upload(UUID.randomUUID(), VALID_BYTES, null, "USER"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }

    @Test
    void upload_unsupportedType_throws400() {
        assertThatThrownBy(() -> profileService.upload(UUID.randomUUID(), VALID_BYTES, "application/pdf", "USER"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }

    @Test
    void upload_fileTooLarge_throws400() {
        byte[] bigFile = new byte[6 * 1024 * 1024];
        assertThatThrownBy(() -> profileService.upload(UUID.randomUUID(), bigFile, JPEG, "USER"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(r2ImageService);
    }
}

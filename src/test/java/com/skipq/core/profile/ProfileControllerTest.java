package com.skipq.core.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock ProfileService profileService;
    @InjectMocks ProfileController controller;

    private UserDetails vendorUser(UUID id) {
        return new User(id.toString(), "", List.of(new SimpleGrantedAuthority("ROLE_VENDOR")));
    }

    private UserDetails adminUser(UUID id) {
        return new User(id.toString(), "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "logo.jpg", "image/jpeg", new byte[100]);
    }

    @Test
    void uploadAvatar_selfUpload_callsUploadAvatar() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileService.uploadAvatar(eq(userId), any(), any())).thenReturn("https://cdn/avatars/" + userId);

        Map<String, String> result = controller.uploadAvatar(vendorUser(userId), jpeg(), null);

        assertThat(result.get("url")).isEqualTo("https://cdn/avatars/" + userId);
        verify(profileService).uploadAvatar(eq(userId), any(), eq("image/jpeg"));
    }

    @Test
    void uploadAvatar_adminUploadsForVendor_callsUploadVendorLogo() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        when(profileService.uploadVendorLogo(eq(vendorId), any(), any())).thenReturn("https://cdn/avatars/" + vendorId);

        Map<String, String> result = controller.uploadAvatar(adminUser(adminId), jpeg(), vendorId);

        assertThat(result.get("url")).isEqualTo("https://cdn/avatars/" + vendorId);
        verify(profileService).uploadVendorLogo(eq(vendorId), any(), eq("image/jpeg"));
    }

    @Test
    void uploadAvatar_nonAdminPassesVendorId_throws403() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> controller.uploadAvatar(vendorUser(userId), jpeg(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }
}

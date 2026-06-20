package com.skipq.core.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock ProfileService profileService;
    @InjectMocks ProfileController controller;

    private MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "logo.jpg", "image/jpeg", new byte[100]);
    }

    @Test
    void upload_vendorType_callsUploadWithVendorType() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.upload(eq(id), any(), any(), eq("VENDOR"))).thenReturn("https://cdn/" + id);

        Map<String, String> result = controller.uploadAvatar(jpeg(), id, "VENDOR");

        assertThat(result.get("url")).isEqualTo("https://cdn/" + id);
        verify(profileService).upload(eq(id), any(), eq("image/jpeg"), eq("VENDOR"));
    }

    @Test
    void upload_userType_callsUploadWithUserType() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.upload(eq(id), any(), any(), eq("USER"))).thenReturn("https://cdn/" + id);

        Map<String, String> result = controller.uploadAvatar(jpeg(), id, "USER");

        assertThat(result.get("url")).isEqualTo("https://cdn/" + id);
        verify(profileService).upload(eq(id), any(), eq("image/jpeg"), eq("USER"));
    }
}

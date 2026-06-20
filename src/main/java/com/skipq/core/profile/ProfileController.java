package com.skipq.core.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR', 'STUDENT', 'ADMIN')")
    public Map<String, String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("id") UUID id,
            @RequestParam("type") String type) throws IOException {

        String url = profileService.upload(id, file.getBytes(), file.getContentType(), type);
        return Map.of("url", url);
    }
}

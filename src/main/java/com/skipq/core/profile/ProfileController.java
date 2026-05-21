package com.skipq.core.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID targetUserId) throws IOException {

        UUID callerId = UUID.fromString(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (targetUserId != null && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can upload on behalf of another user");
        }

        UUID resolvedTarget = (targetUserId != null) ? targetUserId : callerId;

        String url = profileService.uploadAvatar(resolvedTarget, file.getBytes(), file.getContentType());
        return Map.of("url", url);
    }
}

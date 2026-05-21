package com.skipq.core.profile;

import com.skipq.core.auth.UserRepository;
import com.skipq.core.config.R2ImageService;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final long MAX_BYTES = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final R2ImageService r2ImageService;

    @Transactional
    public String uploadVendorLogo(UUID vendorId, byte[] bytes, String contentType) {
        validate(bytes, contentType);
        String url = r2ImageService.uploadAvatar(vendorId, bytes, contentType);
        vendorRepository.updateLogoUrl(vendorId, url);
        return url;
    }

    @Transactional
    public String uploadAvatar(UUID userId, byte[] bytes, String contentType) {
        validate(bytes, contentType);
        String url = r2ImageService.uploadAvatar(userId, bytes, contentType);
        userRepository.updateAvatarUrl(userId, url);
        return url;
    }

    private void validate(byte[] bytes, String contentType) {
        if (bytes.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be under 5 MB");
        }
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG and WebP images are accepted");
        }
    }
}

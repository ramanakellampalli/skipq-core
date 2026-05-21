package com.skipq.core.profile;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.R2ImageService;
import com.skipq.core.vendor.Vendor;
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
    public String uploadAvatar(UUID targetUserId, byte[] bytes, String contentType) {
        if (bytes.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be under 5 MB");
        }
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG and WebP images are accepted");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String url = r2ImageService.uploadAvatar(targetUserId, bytes, contentType);

        if (target.getRole() == UserRole.VENDOR) {
            Vendor vendor = vendorRepository.findByUserId(targetUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
            vendor.setLogoUrl(url);
            vendorRepository.save(vendor);
        } else {
            target.setAvatarUrl(url);
            userRepository.save(target);
        }

        return url;
    }
}

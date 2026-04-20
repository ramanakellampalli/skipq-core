package com.skipq.core.vendor;

import com.skipq.core.vendor.dto.UpdateVendorRequest;
import com.skipq.core.vendor.dto.VendorDashboardResponse;
import com.skipq.core.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping("/api/v1/vendor/sync")
    @PreAuthorize("hasRole('VENDOR')")
    public VendorDashboardResponse sync(@AuthenticationPrincipal UserDetails userDetails) {
        return vendorService.sync(userId(userDetails));
    }

    @GetMapping("/api/v1/vendor/profile")
    @PreAuthorize("hasRole('VENDOR')")
    public VendorResponse getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return vendorService.getProfile(userId(userDetails));
    }

    @PatchMapping("/api/v1/vendor/profile")
    @PreAuthorize("hasRole('VENDOR')")
    public VendorResponse updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                        @Valid @RequestBody UpdateVendorRequest request) {
        return vendorService.updateProfile(userId(userDetails), request);
    }

    @GetMapping("/api/v1/vendors")
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public List<VendorResponse> getOpenVendors() {
        return vendorService.getOpenVendors();
    }

    @GetMapping("/api/v1/vendors/{vendorId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'VENDOR')")
    public VendorResponse getVendor(@PathVariable UUID vendorId) {
        return vendorService.getById(vendorId);
    }

    @DeleteMapping("/api/v1/vendor/account")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        vendorService.deleteAccount(userId(userDetails));
    }

    private UUID userId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}

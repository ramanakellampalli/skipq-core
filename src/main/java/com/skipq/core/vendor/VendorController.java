package com.skipq.core.vendor;

import com.skipq.core.vendor.dto.UpdateVendorRequest;
import com.skipq.core.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    // Vendor app — profile management
    @GetMapping("/api/v1/vendor/profile")
    @PreAuthorize("hasRole('VENDOR')")
    public VendorResponse getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return vendorService.getProfile(userDetails.getUsername());
    }

    @PatchMapping("/api/v1/vendor/profile")
    @PreAuthorize("hasRole('VENDOR')")
    public VendorResponse updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                        @Valid @RequestBody UpdateVendorRequest request) {
        return vendorService.updateProfile(userDetails.getUsername(), request);
    }

    // Student app — browse vendors
    @GetMapping("/api/v1/vendors")
    @PreAuthorize("hasRole('STUDENT')")
    public List<VendorResponse> getOpenVendors() {
        return vendorService.getOpenVendors();
    }

    @GetMapping("/api/v1/vendors/{vendorId}")
    @PreAuthorize("hasRole('STUDENT')")
    public VendorResponse getVendor(@PathVariable UUID vendorId) {
        return vendorService.getById(vendorId);
    }
}

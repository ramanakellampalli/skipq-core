package com.skipq.core.vendor;

import com.skipq.core.vendor.dto.UpdateVendorRequest;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorResponse getProfile(String email) {
        Vendor vendor = findByEmail(email);
        return toResponse(vendor);
    }

    @Transactional
    public VendorResponse updateProfile(String email, UpdateVendorRequest request) {
        Vendor vendor = findByEmail(email);

        if (request.isOpen() != null) {
            vendor.setOpen(request.isOpen());
        }
        if (request.prepTime() != null) {
            vendor.setPrepTime(request.prepTime());
        }

        return toResponse(vendorRepository.save(vendor));
    }

    public List<VendorResponse> getOpenVendors() {
        return vendorRepository.findAllByIsOpenTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VendorResponse getById(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private Vendor findByEmail(String email) {
        return vendorRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user"));
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(vendor.getId(), vendor.getName(), vendor.isOpen(), vendor.getPrepTime());
    }
}

package com.skipq.core.admin;

import com.skipq.core.admin.dto.AdminSyncResponse;
import com.skipq.core.admin.dto.CreateCampusRequest;
import com.skipq.core.admin.dto.CreateVendorRequest;
import com.skipq.core.admin.dto.UpdateVendorStatusRequest;
import com.skipq.core.campus.dto.CampusResponse;
import com.skipq.core.config.R2ImageService;
import com.skipq.core.subscription.dto.RecordSubscriptionPaymentRequest;
import com.skipq.core.subscription.dto.SubscriptionPaymentResponse;
import com.skipq.core.subscription.dto.UpdateSubscriptionRequest;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.support.dto.AdminServiceRequestResponse;
import com.skipq.core.support.dto.UpdateServiceRequestRequest;
import com.skipq.core.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final R2ImageService r2ImageService;
    private final ServiceRequestService serviceRequestService;

    @GetMapping("/sync")
    public AdminSyncResponse sync() {
        return adminService.sync();
    }

    @PostMapping("/campuses")
    @ResponseStatus(HttpStatus.CREATED)
    public CampusResponse createCampus(@Valid @RequestBody CreateCampusRequest request) {
        return adminService.createCampus(request);
    }

    @PostMapping("/vendors")
    @ResponseStatus(HttpStatus.CREATED)
    public void createVendor(@Valid @RequestBody CreateVendorRequest request) {
        adminService.createVendor(request);
    }

    @PutMapping("/support/{id}")
    public AdminServiceRequestResponse updateServiceRequest(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequestRequest request) {
        return serviceRequestService.update(id, request);
    }

    @PutMapping("/vendors/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateVendorStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorStatusRequest request) {
        adminService.updateVendorStatus(id, request);
    }

    @GetMapping("/vendors")
    public List<VendorResponse> getVendors(
            @RequestParam(required = false) String subscriptionStatus) {
        return adminService.getVendors(subscriptionStatus);
    }

    @PutMapping("/vendors/{id}/subscription")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        adminService.updateSubscription(id, request);
    }

    @PostMapping("/vendors/{id}/subscription/payment")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordSubscriptionPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordSubscriptionPaymentRequest request) {
        adminService.recordSubscriptionPayment(id, request);
    }

    @GetMapping("/vendors/{id}/subscription/payments")
    public List<SubscriptionPaymentResponse> getSubscriptionPayments(@PathVariable UUID id) {
        return adminService.getSubscriptionPayments(id);
    }

    @PostMapping("/r2/refresh-cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refreshR2Cache() {
        r2ImageService.refreshCache();
    }
}

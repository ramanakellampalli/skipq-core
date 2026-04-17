package com.skipq.core.admin;

import com.skipq.core.admin.dto.AdminStatsResponse;
import com.skipq.core.admin.dto.CreateVendorRequest;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/vendors")
    @ResponseStatus(HttpStatus.CREATED)
    public void createVendor(@Valid @RequestBody CreateVendorRequest request) {
        adminService.createVendor(request);
    }

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        return adminService.getStats();
    }

    @GetMapping("/orders")
    public List<OrderResponse> getAllOrders() {
        return adminService.getAllOrders();
    }

    @GetMapping("/vendors")
    public List<VendorResponse> getVendors() {
        return adminService.getVendors();
    }
}

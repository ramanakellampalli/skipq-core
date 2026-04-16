package com.skipq.core.admin;

import com.skipq.core.admin.dto.CreateVendorRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}

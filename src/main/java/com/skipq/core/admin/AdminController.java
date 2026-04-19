package com.skipq.core.admin;

import com.skipq.core.admin.dto.AdminSyncResponse;
import com.skipq.core.admin.dto.CreateCampusRequest;
import com.skipq.core.admin.dto.CreateVendorRequest;
import com.skipq.core.campus.dto.CampusResponse;
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
}

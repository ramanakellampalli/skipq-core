package com.skipq.core.menu;

import com.skipq.core.menu.dto.CreateMenuItemRequest;
import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.menu.dto.UpdateMenuItemRequest;
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
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/api/v1/vendor/menu")
    @PreAuthorize("hasRole('VENDOR')")
    public List<MenuItemResponse> getVendorMenu(@AuthenticationPrincipal UserDetails userDetails) {
        return menuItemService.getVendorMenu(userId(userDetails));
    }

    @PostMapping("/api/v1/vendor/menu")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createItem(@AuthenticationPrincipal UserDetails userDetails,
                                       @Valid @RequestBody CreateMenuItemRequest request) {
        return menuItemService.create(userId(userDetails), request);
    }

    @PatchMapping("/api/v1/vendor/menu/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    public MenuItemResponse updateItem(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable UUID itemId,
                                       @Valid @RequestBody UpdateMenuItemRequest request) {
        return menuItemService.update(userId(userDetails), itemId, request);
    }

    @DeleteMapping("/api/v1/vendor/menu/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable UUID itemId) {
        menuItemService.delete(userId(userDetails), itemId);
    }

    @GetMapping("/api/v1/vendors/{vendorId}/menu")
    @PreAuthorize("hasRole('STUDENT')")
    public List<MenuItemResponse> getAvailableMenu(@PathVariable UUID vendorId) {
        return menuItemService.getAvailableMenu(vendorId);
    }

    private UUID userId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}

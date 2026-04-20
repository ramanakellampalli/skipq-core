package com.skipq.core.menu;

import com.skipq.core.menu.dto.*;
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

    // ── Categories (vendor) ───────────────────────────────────────────────────

    @GetMapping("/api/v1/vendor/menu/categories")
    @PreAuthorize("hasRole('VENDOR')")
    public List<MenuCategoryResponse> getCategories(@AuthenticationPrincipal UserDetails userDetails) {
        return menuItemService.getCategories(userId(userDetails));
    }

    @PostMapping("/api/v1/vendor/menu/categories")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuCategoryResponse createCategory(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody CreateMenuCategoryRequest request) {
        return menuItemService.createCategory(userId(userDetails), request);
    }

    @PatchMapping("/api/v1/vendor/menu/categories/{categoryId}")
    @PreAuthorize("hasRole('VENDOR')")
    public MenuCategoryResponse updateCategory(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable UUID categoryId,
                                               @RequestBody UpdateMenuCategoryRequest request) {
        return menuItemService.updateCategory(userId(userDetails), categoryId, request);
    }

    @DeleteMapping("/api/v1/vendor/menu/categories/{categoryId}")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable UUID categoryId) {
        menuItemService.deleteCategory(userId(userDetails), categoryId);
    }

    // ── Items (vendor) ────────────────────────────────────────────────────────

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
        return menuItemService.createItem(userId(userDetails), request);
    }

    @PatchMapping("/api/v1/vendor/menu/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    public MenuItemResponse updateItem(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable UUID itemId,
                                       @RequestBody UpdateMenuItemRequest request) {
        return menuItemService.updateItem(userId(userDetails), itemId, request);
    }

    @DeleteMapping("/api/v1/vendor/menu/{itemId}")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable UUID itemId) {
        menuItemService.deleteItem(userId(userDetails), itemId);
    }

    // ── Variants (vendor) ─────────────────────────────────────────────────────

    @PostMapping("/api/v1/vendor/menu/{itemId}/variants")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuVariantResponse addVariant(@AuthenticationPrincipal UserDetails userDetails,
                                          @PathVariable UUID itemId,
                                          @Valid @RequestBody CreateMenuVariantRequest request) {
        return menuItemService.addVariant(userId(userDetails), itemId, request);
    }

    @PatchMapping("/api/v1/vendor/menu/{itemId}/variants/{variantId}")
    @PreAuthorize("hasRole('VENDOR')")
    public MenuVariantResponse updateVariant(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable UUID itemId,
                                             @PathVariable UUID variantId,
                                             @RequestBody UpdateMenuVariantRequest request) {
        return menuItemService.updateVariant(userId(userDetails), itemId, variantId, request);
    }

    @DeleteMapping("/api/v1/vendor/menu/{itemId}/variants/{variantId}")
    @PreAuthorize("hasRole('VENDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable UUID itemId,
                              @PathVariable UUID variantId) {
        menuItemService.deleteVariant(userId(userDetails), itemId, variantId);
    }

    // ── Menu browse (student) ─────────────────────────────────────────────────

    @GetMapping("/api/v1/vendors/{vendorId}/menu")
    @PreAuthorize("hasRole('STUDENT')")
    public List<MenuItemResponse> getAvailableMenu(@PathVariable UUID vendorId) {
        return menuItemService.getAvailableMenu(vendorId);
    }

    private UUID userId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}

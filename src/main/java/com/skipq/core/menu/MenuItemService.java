package com.skipq.core.menu;

import com.skipq.core.menu.dto.*;
import com.skipq.core.student.dto.StudentMenuResponse;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final VendorRepository vendorRepository;

    // ── Items ─────────────────────────────────────────────────────────────────

    public List<MenuItemResponse> getVendorMenu(UUID userId) {
        UUID vendorId = vendorId(userId);
        return menuItemRepository.findAllByVendorIdWithVariants(vendorId)
                .stream().map(this::toItemResponse).toList();
    }

    public List<MenuItemResponse> getAvailableMenu(UUID vendorId) {
        return menuItemRepository.findAvailableByVendorIdWithVariants(vendorId)
                .stream().map(this::toItemResponse).toList();
    }

    public StudentMenuResponse getAvailableMenuStructured(UUID vendorId) {
        List<MenuItemResponse> items = menuItemRepository.findAvailableByVendorIdWithVariants(vendorId)
                .stream().map(this::toItemResponse).toList();
        return new StudentMenuResponse(items);
    }

    @Transactional
    public MenuItemResponse createItem(UUID userId, CreateMenuItemRequest req) {
        Vendor vendor = findVendor(userId);

        MenuItem item = MenuItem.builder()
                .vendor(vendor)
                .category(req.category())
                .name(req.name())
                .description(req.description())
                .isVeg(req.isVeg())
                .isAvailable(true)
                .displayOrder(req.displayOrder())
                .price(req.price())
                .build();

        menuItemRepository.save(item);

        if (req.variants() != null && !req.variants().isEmpty()) {
            for (CreateMenuVariantRequest vReq : req.variants()) {
                MenuVariant variant = MenuVariant.builder()
                        .menuItem(item)
                        .label(vReq.label())
                        .price(vReq.price())
                        .isAvailable(true)
                        .displayOrder(vReq.displayOrder())
                        .build();
                item.getVariants().add(variant);
            }
            return toItemResponse(menuItemRepository.save(item));
        }

        return toItemResponse(item);
    }

    @Transactional
    public MenuItemResponse updateItem(UUID userId, UUID itemId, UpdateMenuItemRequest req) {
        UUID vendorId = vendorId(userId);
        MenuItem item = menuItemRepository.findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

        if (req.name() != null)        item.setName(req.name());
        if (req.description() != null)  item.setDescription(req.description());
        if (req.isVeg() != null)        item.setVeg(req.isVeg());
        if (req.isAvailable() != null)  item.setAvailable(req.isAvailable());
        if (req.category() != null)     item.setCategory(req.category());
        if (req.displayOrder() != null) item.setDisplayOrder(req.displayOrder());
        if (req.price() != null)        item.setPrice(req.price());

        if (req.variants() != null) {
            item.getVariants().clear();
            for (CreateMenuVariantRequest vReq : req.variants()) {
                MenuVariant variant = MenuVariant.builder()
                        .menuItem(item)
                        .label(vReq.label())
                        .price(vReq.price())
                        .isAvailable(true)
                        .displayOrder(vReq.displayOrder())
                        .build();
                item.getVariants().add(variant);
            }
        }

        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(UUID userId, UUID itemId) {
        UUID vendorId = vendorId(userId);
        MenuItem item = menuItemRepository.findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        menuItemRepository.delete(item);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID vendorId(UUID userId) {
        return findVendor(userId).getId();
    }

    private Vendor findVendor(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    public MenuItemResponse toItemResponse(MenuItem item) {
        List<MenuVariantResponse> variants = item.getVariants().stream()
                .map(this::toVariantResponse).toList();
        boolean isAvailable = item.isAvailable();
        return new MenuItemResponse(
                item.getId(),
                item.getCategory(),
                item.getName(),
                item.getDescription(),
                item.isVeg(),
                isAvailable,
                item.getDisplayOrder(),
                item.getPrice(),
                variants
        );
    }

    private MenuVariantResponse toVariantResponse(MenuVariant variant) {
        return new MenuVariantResponse(
                variant.getId(),
                variant.getLabel(),
                variant.getPrice(),
                variant.isAvailable()
        );
    }
}

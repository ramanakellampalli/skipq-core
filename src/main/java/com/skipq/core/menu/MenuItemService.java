package com.skipq.core.menu;

import com.skipq.core.menu.dto.*;
import com.skipq.core.student.dto.StudentMenuResponse;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuVariantRepository variantRepository;
    private final VendorRepository vendorRepository;

    // ── Categories ────────────────────────────────────────────────────────────

    public List<MenuCategoryResponse> getCategories(UUID userId) {
        UUID vendorId = vendorId(userId);
        return categoryRepository.findAllByVendorIdOrdered(vendorId)
                .stream().map(c -> toCategoryResponse(c, false)).toList();
    }

    @Transactional
    public MenuCategoryResponse createCategory(UUID userId, CreateMenuCategoryRequest req) {
        Vendor vendor = findVendor(userId);
        MenuCategory category = MenuCategory.builder()
                .vendor(vendor)
                .name(req.name())
                .displayOrder(req.displayOrder())
                .build();
        return toCategoryResponse(categoryRepository.save(category), false);
    }

    @Transactional
    public MenuCategoryResponse updateCategory(UUID userId, UUID categoryId, UpdateMenuCategoryRequest req) {
        UUID vendorId = vendorId(userId);
        MenuCategory category = categoryRepository.findByIdAndVendorId(categoryId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (req.name() != null) category.setName(req.name());
        if (req.displayOrder() != null) category.setDisplayOrder(req.displayOrder());
        return toCategoryResponse(categoryRepository.save(category), false);
    }

    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        UUID vendorId = vendorId(userId);
        MenuCategory category = categoryRepository.findByIdAndVendorId(categoryId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        // items with this category get category_id = NULL (ON DELETE SET NULL in migration)
        categoryRepository.delete(category);
    }

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
        List<MenuCategory> categories = categoryRepository.findAllByVendorIdOrdered(vendorId);
        List<MenuItemResponse> allItems = menuItemRepository.findAvailableByVendorIdWithVariants(vendorId)
                .stream().map(this::toItemResponse).toList();

        Map<UUID, List<MenuItemResponse>> byCategory = new LinkedHashMap<>();
        for (MenuCategory c : categories) byCategory.put(c.getId(), new ArrayList<>());

        List<MenuItemResponse> uncategorized = new ArrayList<>();
        for (MenuItemResponse item : allItems) {
            if (item.categoryId() != null && byCategory.containsKey(item.categoryId())) {
                byCategory.get(item.categoryId()).add(item);
            } else {
                uncategorized.add(item);
            }
        }

        List<MenuCategoryResponse> categoryResponses = categories.stream()
                .map(c -> new MenuCategoryResponse(c.getId(), c.getName(), c.getDisplayOrder(), byCategory.get(c.getId())))
                .toList();

        return new StudentMenuResponse(categoryResponses, uncategorized);
    }

    @Transactional
    public MenuItemResponse createItem(UUID userId, CreateMenuItemRequest req) {
        Vendor vendor = findVendor(userId);
        MenuCategory category = resolveCategory(req.categoryId(), vendor.getId());

        MenuItem item = MenuItem.builder()
                .vendor(vendor)
                .category(category)
                .name(req.name())
                .description(req.description())
                .isVeg(req.isVeg())
                .isAvailable(true)
                .displayOrder(req.displayOrder())
                .price(java.math.BigDecimal.ZERO) // price lives in variants
                .build();

        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateItem(UUID userId, UUID itemId, UpdateMenuItemRequest req) {
        UUID vendorId = vendorId(userId);
        MenuItem item = menuItemRepository.findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

        if (req.name() != null) item.setName(req.name());
        if (req.description() != null) item.setDescription(req.description());
        if (req.isVeg() != null) item.setVeg(req.isVeg());
        if (req.isAvailable() != null) item.setAvailable(req.isAvailable());
        if (req.displayOrder() != null) item.setDisplayOrder(req.displayOrder());
        if (req.categoryId() != null) {
            item.setCategory(resolveCategory(req.categoryId(), vendorId));
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

    // ── Variants ──────────────────────────────────────────────────────────────

    @Transactional
    public MenuVariantResponse addVariant(UUID userId, UUID itemId, CreateMenuVariantRequest req) {
        UUID vendorId = vendorId(userId);
        MenuItem item = menuItemRepository.findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

        MenuVariant variant = MenuVariant.builder()
                .menuItem(item)
                .label(req.label())
                .price(req.price())
                .isAvailable(true)
                .displayOrder(req.displayOrder())
                .build();

        return toVariantResponse(variantRepository.save(variant));
    }

    @Transactional
    public MenuVariantResponse updateVariant(UUID userId, UUID itemId, UUID variantId, UpdateMenuVariantRequest req) {
        UUID vendorId = vendorId(userId);
        // ensure item belongs to this vendor
        menuItemRepository.findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

        MenuVariant variant = variantRepository.findByIdAndVendorId(variantId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));

        if (req.label() != null) variant.setLabel(req.label());
        if (req.price() != null) variant.setPrice(req.price());
        if (req.isAvailable() != null) variant.setAvailable(req.isAvailable());
        if (req.displayOrder() != null) variant.setDisplayOrder(req.displayOrder());

        return toVariantResponse(variantRepository.save(variant));
    }

    @Transactional
    public void deleteVariant(UUID userId, UUID itemId, UUID variantId) {
        UUID vendorId = vendorId(userId);
        menuItemRepository.findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        MenuVariant variant = variantRepository.findByIdAndVendorId(variantId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));
        variantRepository.delete(variant);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID vendorId(UUID userId) {
        return findVendor(userId).getId();
    }

    private Vendor findVendor(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private MenuCategory resolveCategory(UUID categoryId, UUID vendorId) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndVendorId(categoryId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public MenuCategoryResponse toCategoryResponse(MenuCategory category, boolean includeItems) {
        List<MenuItemResponse> items = includeItems
                ? category.getItems().stream().map(this::toItemResponse).toList()
                : List.of();
        return new MenuCategoryResponse(category.getId(), category.getName(), category.getDisplayOrder(), items);
    }

    public MenuItemResponse toItemResponse(MenuItem item) {
        List<MenuVariantResponse> variants = item.getVariants().stream()
                .map(this::toVariantResponse).toList();
        boolean isAvailable = item.isAvailable() && variants.stream().anyMatch(MenuVariantResponse::isAvailable);
        return new MenuItemResponse(
                item.getId(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getName(),
                item.getDescription(),
                item.isVeg(),
                isAvailable,
                item.getDisplayOrder(),
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

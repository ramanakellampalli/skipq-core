package com.skipq.core.menu;

import com.skipq.core.menu.dto.CreateMenuItemRequest;
import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.menu.dto.UpdateMenuItemRequest;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final VendorRepository vendorRepository;

    public List<MenuItemResponse> getVendorMenu(UUID userId) {
        Vendor vendor = findVendorByUserId(userId);
        return menuItemRepository.findAllByVendorId(vendor.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<MenuItemResponse> getAvailableMenu(UUID vendorId) {
        return menuItemRepository.findAllByVendorIdAndIsAvailableTrue(vendorId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MenuItemResponse create(UUID userId, CreateMenuItemRequest request) {
        Vendor vendor = findVendorByUserId(userId);
        MenuItem item = MenuItem.builder()
                .vendor(vendor)
                .name(request.name())
                .price(request.price())
                .isAvailable(true)
                .build();
        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse update(UUID userId, UUID itemId, UpdateMenuItemRequest request) {
        MenuItem item = findOwnedItem(userId, itemId);
        if (request.price() != null) item.setPrice(request.price());
        if (request.isAvailable() != null) item.setAvailable(request.isAvailable());
        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void delete(UUID userId, UUID itemId) {
        MenuItem item = findOwnedItem(userId, itemId);
        menuItemRepository.delete(item);
    }

    private MenuItem findOwnedItem(UUID userId, UUID itemId) {
        Vendor vendor = findVendorByUserId(userId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        if (!item.getVendor().getId().equals(vendor.getId())) {
            throw new IllegalArgumentException("Menu item does not belong to this vendor");
        }
        return item;
    }

    private Vendor findVendorByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getName(), item.getPrice(), item.isAvailable());
    }
}

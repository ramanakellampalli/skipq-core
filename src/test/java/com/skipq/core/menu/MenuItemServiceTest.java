package com.skipq.core.menu;

import com.skipq.core.menu.dto.*;
import com.skipq.core.student.dto.StudentMenuResponse;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock MenuItemRepository menuItemRepository;
    @Mock VendorRepository vendorRepository;

    @InjectMocks MenuItemService menuItemService;

    private UUID userId;
    private UUID vendorId;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendor   = Vendor.builder().id(vendorId).name("Test Stall").build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MenuItem itemWithVariants(boolean itemAvailable, boolean variantAvailable) {
        MenuVariant variant = MenuVariant.builder()
                .id(UUID.randomUUID())
                .label("Regular")
                .price(BigDecimal.valueOf(80))
                .isAvailable(variantAvailable)
                .displayOrder(0)
                .build();

        List<MenuVariant> variants = new ArrayList<>();
        variants.add(variant);

        return MenuItem.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .category("Mains")
                .name("Masala Dosa")
                .description("Crispy dosa")
                .isVeg(true)
                .isAvailable(itemAvailable)
                .displayOrder(0)
                .price(BigDecimal.valueOf(80))
                .variants(variants)
                .build();
    }

    private MenuItem simpleItem(boolean available) {
        return MenuItem.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .category("Beverages")
                .name("Water")
                .isVeg(true)
                .isAvailable(available)
                .displayOrder(0)
                .price(BigDecimal.valueOf(20))
                .variants(new ArrayList<>())
                .build();
    }

    // ── getVendorMenu ─────────────────────────────────────────────────────────

    @Test
    void getVendorMenu_returnsAllItemsMapped() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findAllByVendorIdWithVariants(vendorId)).thenReturn(List.of(item));

        List<MenuItemResponse> result = menuItemService.getVendorMenu(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Masala Dosa");
        assertThat(result.get(0).price()).isEqualByComparingTo("80");
    }

    @Test
    void getVendorMenu_throwsWhenVendorNotFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.getVendorMenu(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor not found");
    }

    // ── getAvailableMenu ──────────────────────────────────────────────────────

    @Test
    void getAvailableMenu_returnsItems() {
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findAvailableByVendorIdWithVariants(vendorId)).thenReturn(List.of(item));

        List<MenuItemResponse> result = menuItemService.getAvailableMenu(vendorId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isTrue();
    }

    // ── getAvailableMenuStructured ────────────────────────────────────────────

    @Test
    void getAvailableMenuStructured_returnsFlatItemList() {
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findAvailableByVendorIdWithVariants(vendorId)).thenReturn(List.of(item));

        StudentMenuResponse result = menuItemService.getAvailableMenuStructured(vendorId);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Masala Dosa");
    }

    // ── createItem ────────────────────────────────────────────────────────────

    @Test
    void createItem_simpleItemNoVariants() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem saved = simpleItem(true);
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(saved);

        CreateMenuItemRequest req = new CreateMenuItemRequest("Water", null, true, "Beverages", 0, BigDecimal.valueOf(20), null);
        MenuItemResponse result = menuItemService.createItem(userId, req);

        assertThat(result.price()).isEqualByComparingTo("20");
        assertThat(result.variants()).isEmpty();
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    void createItem_withVariantsSavesTwice() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem saved = itemWithVariants(true, true);
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(saved);

        CreateMenuVariantRequest variantReq = new CreateMenuVariantRequest("Full", BigDecimal.valueOf(150), 0);
        CreateMenuItemRequest req = new CreateMenuItemRequest("Biryani", null, false, "Mains", 0, BigDecimal.valueOf(80), List.of(variantReq));

        menuItemService.createItem(userId, req);

        verify(menuItemRepository, times(2)).save(any(MenuItem.class));
    }

    @Test
    void createItem_setsRealPriceOnItem() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));

        ArgumentCaptor<MenuItem> captor = ArgumentCaptor.forClass(MenuItem.class);
        MenuItem saved = simpleItem(true);
        when(menuItemRepository.save(captor.capture())).thenReturn(saved);

        CreateMenuItemRequest req = new CreateMenuItemRequest("Tea", null, true, "Beverages", 0, BigDecimal.valueOf(30), null);
        menuItemService.createItem(userId, req);

        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("30");
    }

    @Test
    void createItem_throwsWhenVendorNotFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        CreateMenuItemRequest req = new CreateMenuItemRequest("Tea", null, true, "Beverages", 0, BigDecimal.valueOf(30), null);

        assertThatThrownBy(() -> menuItemService.createItem(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor not found");

        verifyNoInteractions(menuItemRepository);
    }

    // ── updateItem ────────────────────────────────────────────────────────────

    @Test
    void updateItem_updatesNameAndCategory() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        UpdateMenuItemRequest req = new UpdateMenuItemRequest("New Name", null, null, null, "Snacks", null, null, null);
        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.getName()).isEqualTo("New Name");
        assertThat(item.getCategory()).isEqualTo("Snacks");
    }

    @Test
    void updateItem_updatesPrice() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        UpdateMenuItemRequest req = new UpdateMenuItemRequest(null, null, null, null, null, null, BigDecimal.valueOf(95), null);
        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.getPrice()).isEqualByComparingTo("95");
    }

    @Test
    void updateItem_replacesVariantsWhenProvided() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        CreateMenuVariantRequest v1 = new CreateMenuVariantRequest("Small", BigDecimal.valueOf(80), 0);
        CreateMenuVariantRequest v2 = new CreateMenuVariantRequest("Full", BigDecimal.valueOf(150), 1);
        UpdateMenuItemRequest req = new UpdateMenuItemRequest(null, null, null, null, null, null, null, List.of(v1, v2));

        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.getVariants()).hasSize(2);
        assertThat(item.getVariants().get(0).getLabel()).isEqualTo("Small");
    }

    @Test
    void updateItem_clearsVariantsWhenEmptyListProvided() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        UpdateMenuItemRequest req = new UpdateMenuItemRequest(null, null, null, null, null, null, null, List.of());
        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.getVariants()).isEmpty();
    }

    @Test
    void updateItem_keepsVariantsWhenNotProvided() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        UpdateMenuItemRequest req = new UpdateMenuItemRequest("Updated", null, null, null, null, null, null, null);
        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.getVariants()).hasSize(1);
    }

    @Test
    void updateItem_throwsWhenNotFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        UUID itemId = UUID.randomUUID();
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.empty());

        UpdateMenuItemRequest req = new UpdateMenuItemRequest("x", null, null, null, null, null, null, null);
        assertThatThrownBy(() -> menuItemService.updateItem(userId, itemId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Menu item not found");
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test
    void deleteItem_deletesWhenFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));

        menuItemService.deleteItem(userId, item.getId());

        verify(menuItemRepository).delete(item);
    }

    @Test
    void deleteItem_throwsWhenNotFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        UUID itemId = UUID.randomUUID();
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.deleteItem(userId, itemId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Menu item not found");

        verify(menuItemRepository, never()).delete(any());
    }

    // ── toItemResponse ────────────────────────────────────────────────────────

    @Test
    void toItemResponse_includesPrice() {
        MenuItem item = simpleItem(true);
        MenuItemResponse response = menuItemService.toItemResponse(item);
        assertThat(response.price()).isEqualByComparingTo("20");
    }

    @Test
    void toItemResponse_availabilityIsItemFlagOnly() {
        assertThat(menuItemService.toItemResponse(itemWithVariants(true, false)).isAvailable()).isTrue();
        assertThat(menuItemService.toItemResponse(itemWithVariants(false, true)).isAvailable()).isFalse();
        assertThat(menuItemService.toItemResponse(simpleItem(true)).isAvailable()).isTrue();
        assertThat(menuItemService.toItemResponse(simpleItem(false)).isAvailable()).isFalse();
    }

    @Test
    void toItemResponse_mapsAllFieldsCorrectly() {
        MenuItem item = itemWithVariants(true, true);
        MenuItemResponse response = menuItemService.toItemResponse(item);

        assertThat(response.name()).isEqualTo("Masala Dosa");
        assertThat(response.description()).isEqualTo("Crispy dosa");
        assertThat(response.category()).isEqualTo("Mains");
        assertThat(response.isVeg()).isTrue();
        assertThat(response.price()).isEqualByComparingTo("80");
        assertThat(response.variants()).hasSize(1);
    }
}

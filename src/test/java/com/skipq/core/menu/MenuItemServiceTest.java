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
                .price(BigDecimal.ZERO)
                .variants(variants)
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
        assertThat(result.get(0).category()).isEqualTo("Mains");
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
    void getAvailableMenu_returnsOnlyAvailableItems() {
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
    void createItem_savesItemAndVariantsInOneCall() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));

        MenuItem saved = itemWithVariants(true, true);
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(saved);

        CreateMenuVariantRequest variantReq = new CreateMenuVariantRequest(null, BigDecimal.valueOf(80), 0);
        CreateMenuItemRequest req = new CreateMenuItemRequest("Masala Dosa", "Crispy dosa", true, "Mains", 0, List.of(variantReq));

        MenuItemResponse result = menuItemService.createItem(userId, req);

        assertThat(result.name()).isEqualTo("Masala Dosa");
        assertThat(result.category()).isEqualTo("Mains");
        assertThat(result.variants()).hasSize(1);

        // item saved twice: once to get ID, once to cascade variants
        verify(menuItemRepository, times(2)).save(any(MenuItem.class));
    }

    @Test
    void createItem_buildsVariantsOnItemBeforeSecondSave() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));

        ArgumentCaptor<MenuItem> captor = ArgumentCaptor.forClass(MenuItem.class);
        MenuItem saved = itemWithVariants(true, true);
        when(menuItemRepository.save(captor.capture())).thenReturn(saved);

        CreateMenuVariantRequest variantReq = new CreateMenuVariantRequest("Full", BigDecimal.valueOf(120), 0);
        CreateMenuItemRequest req = new CreateMenuItemRequest("Filter Coffee", null, true, "Beverages", 0, List.of(variantReq));

        menuItemService.createItem(userId, req);

        // second captured save should have the variant in the list
        MenuItem secondSave = captor.getAllValues().get(1);
        assertThat(secondSave.getVariants()).hasSize(1);
        assertThat(secondSave.getVariants().get(0).getLabel()).isEqualTo("Full");
        assertThat(secondSave.getVariants().get(0).getPrice()).isEqualByComparingTo("120");
    }

    @Test
    void createItem_throwsWhenVendorNotFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        CreateMenuVariantRequest variantReq = new CreateMenuVariantRequest(null, BigDecimal.valueOf(50), 0);
        CreateMenuItemRequest req = new CreateMenuItemRequest("Tea", null, true, "Beverages", 0, List.of(variantReq));

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

        UpdateMenuItemRequest req = new UpdateMenuItemRequest("New Name", null, null, null, "Snacks", null, null);
        MenuItemResponse result = menuItemService.updateItem(userId, item.getId(), req);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.category()).isEqualTo("Snacks");
        verify(menuItemRepository).save(item);
    }

    @Test
    void updateItem_togglesAvailability() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        UpdateMenuItemRequest req = new UpdateMenuItemRequest(null, null, null, false, null, null, null);
        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.isAvailable()).isFalse();
    }

    @Test
    void updateItem_replacesVariantsWhenProvided() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        CreateMenuVariantRequest v1 = new CreateMenuVariantRequest("Half", BigDecimal.valueOf(60), 0);
        CreateMenuVariantRequest v2 = new CreateMenuVariantRequest("Full", BigDecimal.valueOf(100), 1);
        UpdateMenuItemRequest req = new UpdateMenuItemRequest(null, null, null, null, null, null, List.of(v1, v2));

        menuItemService.updateItem(userId, item.getId(), req);

        assertThat(item.getVariants()).hasSize(2);
        assertThat(item.getVariants().get(0).getLabel()).isEqualTo("Half");
        assertThat(item.getVariants().get(1).getLabel()).isEqualTo("Full");
    }

    @Test
    void updateItem_keepsExistingVariantsWhenNotProvided() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        MenuItem item = itemWithVariants(true, true);
        when(menuItemRepository.findByIdAndVendorId(item.getId(), vendorId)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(item)).thenReturn(item);

        UpdateMenuItemRequest req = new UpdateMenuItemRequest("Updated Name", null, null, null, null, null, null);
        menuItemService.updateItem(userId, item.getId(), req);

        // variants unchanged
        assertThat(item.getVariants()).hasSize(1);
        assertThat(item.getVariants().get(0).getLabel()).isEqualTo("Regular");
    }

    @Test
    void updateItem_throwsWhenNotFound() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        UUID itemId = UUID.randomUUID();
        when(menuItemRepository.findByIdAndVendorId(itemId, vendorId)).thenReturn(Optional.empty());

        UpdateMenuItemRequest req = new UpdateMenuItemRequest("x", null, null, null, null, null, null);
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

    // ── toItemResponse — availability logic ───────────────────────────────────

    @Test
    void toItemResponse_isAvailableTrueWhenItemAndVariantBothAvailable() {
        MenuItem item = itemWithVariants(true, true);
        MenuItemResponse response = menuItemService.toItemResponse(item);
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void toItemResponse_isAvailableFalseWhenItemUnavailable() {
        MenuItem item = itemWithVariants(false, true);
        MenuItemResponse response = menuItemService.toItemResponse(item);
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void toItemResponse_isAvailableFalseWhenNoAvailableVariants() {
        MenuItem item = itemWithVariants(true, false);
        MenuItemResponse response = menuItemService.toItemResponse(item);
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void toItemResponse_mapsAllFieldsCorrectly() {
        MenuItem item = itemWithVariants(true, true);
        MenuItemResponse response = menuItemService.toItemResponse(item);

        assertThat(response.id()).isEqualTo(item.getId());
        assertThat(response.name()).isEqualTo("Masala Dosa");
        assertThat(response.description()).isEqualTo("Crispy dosa");
        assertThat(response.category()).isEqualTo("Mains");
        assertThat(response.isVeg()).isTrue();
        assertThat(response.variants()).hasSize(1);
        assertThat(response.variants().get(0).price()).isEqualByComparingTo("80");
    }

    @Test
    void toItemResponse_nullCategoryReturnedAsNull() {
        MenuItem item = MenuItem.builder()
                .id(UUID.randomUUID()).vendor(vendor).category(null)
                .name("Plain Rice").isVeg(true).isAvailable(true).displayOrder(0)
                .price(BigDecimal.ZERO).variants(new ArrayList<>()).build();

        MenuItemResponse response = menuItemService.toItemResponse(item);

        assertThat(response.category()).isNull();
        assertThat(response.isAvailable()).isFalse(); // no variants → unavailable
    }
}

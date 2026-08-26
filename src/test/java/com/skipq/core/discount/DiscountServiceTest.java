package com.skipq.core.discount;

import com.skipq.core.discount.dto.AttachItemsRequest;
import com.skipq.core.discount.dto.CreateDiscountRequest;
import com.skipq.core.discount.dto.DiscountResponse;
import com.skipq.core.discount.dto.UpdateDiscountRequest;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock DiscountRepository discountRepository;
    @Mock VendorRepository vendorRepository;
    @Mock MenuItemRepository menuItemRepository;
    @InjectMocks DiscountService discountService;

    private UUID userId;
    private UUID vendorId;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendor   = Vendor.builder().id(vendorId).name("Test Stall").build();
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_returnsDiscountsForVendor() {
        Discount d = discount(DiscountType.PERCENTAGE, "10");
        when(discountRepository.findAllByVendorIdAndDeletedAtIsNullOrderByCreatedAtDesc(vendorId))
                .thenReturn(List.of(d));

        List<DiscountResponse> result = discountService.list(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Summer Sale");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_percentage_savesAndReturnsDiscount() {
        var req = new CreateDiscountRequest("Flash", DiscountType.PERCENTAGE, new BigDecimal("15"), null, null);
        Discount saved = discount(DiscountType.PERCENTAGE, "15");
        when(discountRepository.save(any(Discount.class))).thenReturn(saved);

        DiscountResponse result = discountService.create(userId, req);

        assertThat(result.type()).isEqualTo(DiscountType.PERCENTAGE);
        ArgumentCaptor<Discount> cap = ArgumentCaptor.forClass(Discount.class);
        verify(discountRepository).save(cap.capture());
        assertThat(cap.getValue().getVendor()).isEqualTo(vendor);
        assertThat(cap.getValue().isActive()).isTrue();
        assertThat(cap.getValue().getScope()).isEqualTo(DiscountScope.ITEM);
    }

    @Test
    void create_percentageOver100_throws400() {
        var req = new CreateDiscountRequest("Bad", DiscountType.PERCENTAGE, new BigDecimal("101"), null, null);

        assertThatThrownBy(() -> discountService.create(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void create_endsAtBeforeStartsAt_throws400() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end   = LocalDateTime.now().plusDays(1);
        var req = new CreateDiscountRequest("Bad", DiscountType.FIXED_AMOUNT, new BigDecimal("10"), start, end);

        assertThatThrownBy(() -> discountService.create(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void create_vendorNotFound_throws404() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        var req = new CreateDiscountRequest("X", DiscountType.PERCENTAGE, new BigDecimal("10"), null, null);

        assertThatThrownBy(() -> discountService.create(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(404);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_name_updatesName() {
        Discount d = ownedDiscount();
        when(discountRepository.save(d)).thenReturn(d);

        discountService.update(userId, d.getId(), new UpdateDiscountRequest("New Name", null, null, null));

        verify(discountRepository).save(argThat(x -> x.getName().equals("New Name")));
    }

    @Test
    void update_toggleActiveOff_deactivates() {
        Discount d = ownedDiscount();
        when(discountRepository.save(d)).thenReturn(d);

        discountService.update(userId, d.getId(), new UpdateDiscountRequest(null, false, null, null));

        verify(discountRepository).save(argThat(x -> !x.isActive()));
    }

    @Test
    void update_wrongVendor_throws403() {
        UUID otherId = UUID.randomUUID();
        Vendor other = Vendor.builder().id(otherId).name("Other").build();
        Discount d = Discount.builder()
                .id(UUID.randomUUID())
                .vendor(other)
                .name("Theirs")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .scope(DiscountScope.ITEM)
                .active(true)
                .priority(0)
                .build();
        when(discountRepository.findById(d.getId())).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> discountService.update(userId, d.getId(), new UpdateDiscountRequest("X", null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void update_deletedDiscount_throws404() {
        Discount d = Discount.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .name("Gone")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .scope(DiscountScope.ITEM)
                .active(false)
                .priority(0)
                .deletedAt(LocalDateTime.now())
                .build();
        when(discountRepository.findById(d.getId())).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> discountService.update(userId, d.getId(), new UpdateDiscountRequest("X", null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(404);
    }

    // ── attachItems ───────────────────────────────────────────────────────────

    @Test
    void attachItems_validItem_addsToDiscount() {
        Discount d     = ownedDiscount();
        MenuItem item  = menuItem(vendor);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(discountRepository.hasOtherActiveDiscount(eq(item.getId()), eq(d.getId()), any())).thenReturn(false);

        discountService.attachItems(userId, d.getId(), new AttachItemsRequest(List.of(item.getId())));

        assertThat(d.getMenuItems()).contains(item);
        verify(discountRepository).save(d);
    }

    @Test
    void attachItems_itemFromOtherVendor_throws400() {
        Discount d    = ownedDiscount();
        Vendor other  = Vendor.builder().id(UUID.randomUUID()).name("Other").build();
        MenuItem item = menuItem(other);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> discountService.attachItems(userId, d.getId(), new AttachItemsRequest(List.of(item.getId()))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void attachItems_itemAlreadyHasActiveDiscount_throws409() {
        Discount d    = ownedDiscount();
        MenuItem item = menuItem(vendor);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(discountRepository.hasOtherActiveDiscount(eq(item.getId()), eq(d.getId()), any())).thenReturn(true);

        assertThatThrownBy(() -> discountService.attachItems(userId, d.getId(), new AttachItemsRequest(List.of(item.getId()))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(409);
    }

    // ── detachItem ────────────────────────────────────────────────────────────

    @Test
    void detachItem_removesItemFromDiscount() {
        Discount d    = ownedDiscount();
        MenuItem item = menuItem(vendor);
        d.addMenuItem(item);

        discountService.detachItem(userId, d.getId(), item.getId());

        assertThat(d.getMenuItems()).doesNotContain(item);
        verify(discountRepository).save(d);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_softDeletesSetsDeletedAtAndDeactivates() {
        Discount d = ownedDiscount();

        discountService.delete(userId, d.getId());

        ArgumentCaptor<Discount> cap = ArgumentCaptor.forClass(Discount.class);
        verify(discountRepository).save(cap.capture());
        assertThat(cap.getValue().isActive()).isFalse();
        assertThat(cap.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void delete_doesNotPhysicallyDelete() {
        Discount d = ownedDiscount();

        discountService.delete(userId, d.getId());

        verify(discountRepository, never()).delete(any(Discount.class));
        verify(discountRepository, never()).deleteById(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Discount discount(DiscountType type, String value) {
        return Discount.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .name("Summer Sale")
                .type(type)
                .value(new BigDecimal(value))
                .scope(DiscountScope.ITEM)
                .active(true)
                .priority(0)
                .build();
    }

    private Discount ownedDiscount() {
        Discount d = discount(DiscountType.PERCENTAGE, "10");
        when(discountRepository.findById(d.getId())).thenReturn(Optional.of(d));
        return d;
    }

    private MenuItem menuItem(Vendor owner) {
        return MenuItem.builder()
                .id(UUID.randomUUID())
                .vendor(owner)
                .name("Masala Dosa")
                .price(new BigDecimal("60.00"))
                .isVeg(true)
                .isAvailable(true)
                .displayOrder(0)
                .build();
    }
}

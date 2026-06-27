package com.skipq.core.discount;

import com.skipq.core.discount.dto.AttachItemsRequest;
import com.skipq.core.discount.dto.CreateDiscountRequest;
import com.skipq.core.discount.dto.DiscountResponse;
import com.skipq.core.discount.dto.UpdateDiscountRequest;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final VendorRepository vendorRepository;
    private final MenuItemRepository menuItemRepository;

    @Transactional(readOnly = true)
    public List<DiscountResponse> list(UUID userId) {
        UUID vendorId = vendorId(userId);
        return discountRepository.findAllByVendorIdAndDeletedAtIsNullOrderByCreatedAtDesc(vendorId)
                .stream().map(DiscountResponse::from).toList();
    }

    @Transactional
    public DiscountResponse create(UUID userId, CreateDiscountRequest req) {
        Vendor vendor = vendor(userId);
        validatePercentage(req.type(), req.value());
        validateDateRange(req.startsAt(), req.endsAt());

        Discount discount = Discount.builder()
                .vendor(vendor)
                .name(req.name())
                .type(req.type())
                .value(req.value())
                .scope(DiscountScope.ITEM)
                .active(true)
                .priority(0)
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .build();

        return DiscountResponse.from(discountRepository.save(discount));
    }

    @Transactional
    public DiscountResponse update(UUID userId, UUID discountId, UpdateDiscountRequest req) {
        Discount discount = ownedDiscount(userId, discountId);

        if (req.name()     != null) discount.setName(req.name());
        if (req.active()   != null) discount.setActive(req.active());
        if (req.startsAt() != null) discount.setStartsAt(req.startsAt());
        if (req.endsAt()   != null) discount.setEndsAt(req.endsAt());

        validateDateRange(discount.getStartsAt(), discount.getEndsAt());
        return DiscountResponse.from(discountRepository.save(discount));
    }

    @Transactional
    public void attachItems(UUID userId, UUID discountId, AttachItemsRequest req) {
        Discount discount = ownedDiscount(userId, discountId);
        UUID vendorId = discount.getVendor().getId();
        LocalDateTime now = LocalDateTime.now();

        for (UUID menuItemId : req.menuItemIds()) {
            MenuItem item = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Menu item not found: " + menuItemId));

            if (!item.getVendor().getId().equals(vendorId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Menu item does not belong to your store: " + menuItemId);
            }

            if (discountRepository.hasOtherActiveDiscount(menuItemId, discountId, now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Item already has an active discount: " + item.getName());
            }

            discount.getMenuItems().add(item);
        }

        discountRepository.save(discount);
    }

    @Transactional
    public void detachItem(UUID userId, UUID discountId, UUID menuItemId) {
        Discount discount = ownedDiscount(userId, discountId);
        discount.getMenuItems().removeIf(m -> m.getId().equals(menuItemId));
        discountRepository.save(discount);
    }

    @Transactional
    public void delete(UUID userId, UUID discountId) {
        Discount discount = ownedDiscount(userId, discountId);
        discount.setActive(false);
        discount.setDeletedAt(LocalDateTime.now());
        discountRepository.save(discount);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Discount ownedDiscount(UUID userId, UUID discountId) {
        UUID vendorId = vendorId(userId);
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found"));
        if (!discount.getVendor().getId().equals(vendorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Discount does not belong to your store");
        }
        if (discount.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found");
        }
        return discount;
    }

    private UUID vendorId(UUID userId) {
        return vendor(userId).getId();
    }

    private Vendor vendor(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
    }

    private void validatePercentage(DiscountType type, java.math.BigDecimal value) {
        if (type == DiscountType.PERCENTAGE && value.compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        }
    }

    private void validateDateRange(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ends_at must be after starts_at");
        }
    }
}

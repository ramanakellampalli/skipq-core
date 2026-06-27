package com.skipq.core.discount;

import com.skipq.core.menu.MenuItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceResolverTest {

    @Mock DiscountRepository discountRepository;
    @InjectMocks PriceResolver priceResolver;

    private MenuItem item(BigDecimal price) {
        return MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Test Item")
                .price(price)
                .isVeg(true)
                .isAvailable(true)
                .displayOrder(0)
                .build();
    }

    private Discount discount(DiscountType type, String value) {
        return Discount.builder()
                .id(UUID.randomUUID())
                .name("Test Discount")
                .type(type)
                .value(new BigDecimal(value))
                .scope(DiscountScope.ITEM)
                .active(true)
                .priority(0)
                .build();
    }

    private void stubDiscount(UUID menuItemId, Discount discount) {
        when(discountRepository.findActiveByMenuItemId(eq(menuItemId), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(discount));
    }

    private void stubNoDiscount(UUID menuItemId) {
        when(discountRepository.findActiveByMenuItemId(eq(menuItemId), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());
    }

    // ── PERCENTAGE ────────────────────────────────────────────────────────────

    @Test
    void resolve_percentage_reducesPrice() {
        MenuItem item = item(new BigDecimal("100.00"));
        stubDiscount(item.getId(), discount(DiscountType.PERCENTAGE, "10"));

        ResolvedPrice result = priceResolver.resolve(item, LocalDateTime.now());

        assertThat(result.discountedPrice()).isEqualByComparingTo("90.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(result.originalPrice()).isEqualByComparingTo("100.00");
        assertThat(result.hasDiscount()).isTrue();
    }

    @Test
    void resolve_percentage_roundsHalfUp() {
        MenuItem item = item(new BigDecimal("100.00"));
        // 33.33% of 100 = 33.33 reduction → discountedPrice = 66.67
        stubDiscount(item.getId(), discount(DiscountType.PERCENTAGE, "33.33"));

        ResolvedPrice result = priceResolver.resolve(item, LocalDateTime.now());

        assertThat(result.discountedPrice()).isEqualByComparingTo("66.67");
        assertThat(result.discountAmount()).isEqualByComparingTo("33.33");
    }

    @Test
    void resolve_percentage100_floorsAtMinPrice() {
        MenuItem item = item(new BigDecimal("50.00"));
        stubDiscount(item.getId(), discount(DiscountType.PERCENTAGE, "100"));

        ResolvedPrice result = priceResolver.resolve(item, LocalDateTime.now());

        assertThat(result.discountedPrice()).isEqualByComparingTo("0.01");
        assertThat(result.hasDiscount()).isTrue();
    }

    // ── FIXED_AMOUNT ──────────────────────────────────────────────────────────

    @Test
    void resolve_fixedAmount_reducesPrice() {
        MenuItem item = item(new BigDecimal("80.00"));
        stubDiscount(item.getId(), discount(DiscountType.FIXED_AMOUNT, "20"));

        ResolvedPrice result = priceResolver.resolve(item, LocalDateTime.now());

        assertThat(result.discountedPrice()).isEqualByComparingTo("60.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(result.originalPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    void resolve_fixedAmountExceedsPrice_floorsAtMinPrice() {
        MenuItem item = item(new BigDecimal("30.00"));
        stubDiscount(item.getId(), discount(DiscountType.FIXED_AMOUNT, "200"));

        ResolvedPrice result = priceResolver.resolve(item, LocalDateTime.now());

        assertThat(result.discountedPrice()).isEqualByComparingTo("0.01");
    }

    // ── no discount ───────────────────────────────────────────────────────────

    @Test
    void resolve_noActiveDiscount_returnsOriginalPrice() {
        MenuItem item = item(new BigDecimal("45.00"));
        stubNoDiscount(item.getId());

        ResolvedPrice result = priceResolver.resolve(item, LocalDateTime.now());

        assertThat(result.discountedPrice()).isEqualByComparingTo("45.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.discount()).isNull();
        assertThat(result.hasDiscount()).isFalse();
    }

    // ── variant price overload ────────────────────────────────────────────────

    @Test
    void resolve_withVariantPrice_appliesDiscountToVariantPrice() {
        MenuItem item = item(new BigDecimal("50.00")); // base price
        BigDecimal variantPrice = new BigDecimal("120.00");
        stubDiscount(item.getId(), discount(DiscountType.PERCENTAGE, "10"));

        ResolvedPrice result = priceResolver.resolve(item, variantPrice, LocalDateTime.now());

        // 10% of ₹120 = ₹12 off → ₹108
        assertThat(result.originalPrice()).isEqualByComparingTo("120.00");
        assertThat(result.discountedPrice()).isEqualByComparingTo("108.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("12.00");
    }

    // ── discountLabel ─────────────────────────────────────────────────────────

    @Test
    void discountLabel_percentage_formatsCorrectly() {
        Discount d = discount(DiscountType.PERCENTAGE, "10");
        assertThat(priceResolver.discountLabel(d)).isEqualTo("10% off");
    }

    @Test
    void discountLabel_fixedAmount_formatsWithRupeeSymbol() {
        Discount d = discount(DiscountType.FIXED_AMOUNT, "25.00");
        assertThat(priceResolver.discountLabel(d)).isEqualTo("₹25 off");
    }
}

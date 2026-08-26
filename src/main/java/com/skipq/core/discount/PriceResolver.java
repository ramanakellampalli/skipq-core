package com.skipq.core.discount;

import com.skipq.core.menu.MenuItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Single source of truth for all discount arithmetic.
 * Every caller that needs a price must go through here — never compute discounts inline.
 */
@Component
@RequiredArgsConstructor
public class PriceResolver {

    private static final BigDecimal MIN_PRICE    = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED      = new BigDecimal("100");
    private static final PageRequest FIRST       = PageRequest.of(0, 1);

    private final DiscountRepository discountRepository;

    /**
     * Resolves the effective price for a menu item using the given effective price
     * (either item base price or variant price, whichever applies for this order item).
     */
    public ResolvedPrice resolve(MenuItem item, BigDecimal effectivePrice, LocalDateTime now) {
        List<Discount> hits = discountRepository.findActiveByMenuItemId(item.getId(), now, FIRST);
        if (hits.isEmpty()) {
            return new ResolvedPrice(effectivePrice, effectivePrice, BigDecimal.ZERO, null);
        }
        Discount discount = hits.get(0);
        BigDecimal discountedPrice = apply(effectivePrice, discount);
        BigDecimal discountAmount  = effectivePrice.subtract(discountedPrice).setScale(2, RoundingMode.HALF_UP);
        return new ResolvedPrice(effectivePrice, discountedPrice, discountAmount, discount);
    }

    /** Convenience overload — uses item base price as the effective price. */
    public ResolvedPrice resolve(MenuItem item, LocalDateTime now) {
        return resolve(item, item.getPrice(), now);
    }

    /** Builds the label shown to customers, e.g. "10% off" or "₹20 off". */
    public String discountLabel(Discount discount) {
        return switch (discount.getType()) {
            case PERCENTAGE   -> discount.getValue().stripTrailingZeros().toPlainString() + "% off";
            case FIXED_AMOUNT -> "₹" + discount.getValue().stripTrailingZeros().toPlainString() + " off";
        };
    }

    /** Resolves using a pre-fetched discount — avoids a second DB hit from OrderService. */
    public ResolvedPrice resolveWithDiscount(BigDecimal effectivePrice, Discount discount) {
        BigDecimal discountedPrice = apply(effectivePrice, discount);
        BigDecimal discountAmount  = effectivePrice.subtract(discountedPrice).setScale(2, RoundingMode.HALF_UP);
        return new ResolvedPrice(effectivePrice, discountedPrice, discountAmount, discount);
    }

    public ResolvedPrice resolveForMenuItemId(UUID menuItemId, BigDecimal effectivePrice, LocalDateTime now) {
        List<Discount> hits = discountRepository.findActiveByMenuItemId(menuItemId, now, FIRST);
        if (hits.isEmpty()) {
            return new ResolvedPrice(effectivePrice, effectivePrice, BigDecimal.ZERO, null);
        }
        return resolveWithDiscount(effectivePrice, hits.get(0));
    }

    private BigDecimal apply(BigDecimal price, Discount discount) {
        return switch (discount.getType()) {
            case PERCENTAGE -> {
                BigDecimal reduction = price.multiply(discount.getValue())
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                yield price.subtract(reduction).max(MIN_PRICE).setScale(2, RoundingMode.HALF_UP);
            }
            case FIXED_AMOUNT ->
                    price.subtract(discount.getValue()).max(MIN_PRICE).setScale(2, RoundingMode.HALF_UP);
        };
    }
}

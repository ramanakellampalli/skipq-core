package com.skipq.core.discount;

import java.math.BigDecimal;

/**
 * Result of resolving a price through PriceResolver.
 * All pricing decisions flow through this record — never compute prices inline elsewhere.
 */
public record ResolvedPrice(
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        BigDecimal discountAmount,
        Discount   discount          // null when no active discount
) {
    public boolean hasDiscount() {
        return discount != null;
    }
}

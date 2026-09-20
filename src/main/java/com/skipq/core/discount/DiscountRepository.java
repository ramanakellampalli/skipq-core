package com.skipq.core.discount;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {

    List<Discount> findAllByVendorIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID vendorId);

    @Query("""
            SELECT d FROM Discount d
            JOIN d.menuItems m
            WHERE m.id = :menuItemId
              AND d.active = true
              AND d.deletedAt IS NULL
              AND (d.startsAt IS NULL OR d.startsAt <= :now)
              AND (d.endsAt   IS NULL OR d.endsAt   >= :now)
            ORDER BY d.priority DESC
            """)
    List<Discount> findActiveByMenuItemId(@Param("menuItemId") UUID menuItemId,
                                          @Param("now") LocalDateTime now,
                                          Pageable pageable);

    @Query("""
            SELECT COUNT(d) > 0 FROM Discount d
            JOIN d.menuItems m
            WHERE m.id = :menuItemId
              AND d.id != :excludeId
              AND d.active = true
              AND d.deletedAt IS NULL
              AND (d.startsAt IS NULL OR d.startsAt <= :now)
              AND (d.endsAt   IS NULL OR d.endsAt   >= :now)
            """)
    boolean hasOtherActiveDiscount(@Param("menuItemId") UUID menuItemId,
                                   @Param("excludeId") UUID excludeId,
                                   @Param("now") LocalDateTime now);
}

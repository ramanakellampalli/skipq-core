package com.skipq.core.settlement;

import com.skipq.core.common.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    boolean existsByOrderIdAndType(UUID orderId, LedgerEntryType type);

    @Query("""
            SELECT le.vendorId, SUM(le.amount)
            FROM LedgerEntry le
            WHERE le.settled = false AND le.createdAt <= :cutoff
            GROUP BY le.vendorId
            HAVING SUM(le.amount) > 0
            """)
    List<Object[]> sumUnsettledByVendorBeforeCutoff(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("""
            UPDATE LedgerEntry le
            SET le.payoutId = :payoutId, le.settled = true
            WHERE le.vendorId = :vendorId
              AND le.settled = false
              AND le.createdAt <= :cutoff
            """)
    void markSettled(@Param("vendorId") UUID vendorId,
                     @Param("payoutId") UUID payoutId,
                     @Param("cutoff") LocalDateTime cutoff);
}

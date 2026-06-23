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

    // Only considers entries not yet reserved for a payout (payout_id IS NULL).
    // Prevents the same entries being counted in a duplicate payout run.
    @Query("""
            SELECT le.vendorId, SUM(le.amount)
            FROM LedgerEntry le
            WHERE le.settled = false
              AND le.payoutId IS NULL
              AND le.createdAt <= :cutoff
            GROUP BY le.vendorId
            HAVING SUM(le.amount) > 0
            """)
    List<Object[]> sumUnsettledByVendorBeforeCutoff(@Param("cutoff") LocalDateTime cutoff);

    // Reserve entries for a specific payout at job creation time.
    // Entries remain settled=false until admin confirms the transfer.
    @Modifying
    @Query("""
            UPDATE LedgerEntry le
            SET le.payoutId = :payoutId
            WHERE le.vendorId = :vendorId
              AND le.settled = false
              AND le.payoutId IS NULL
              AND le.createdAt <= :cutoff
            """)
    void reserveForPayout(@Param("vendorId") UUID vendorId,
                          @Param("payoutId") UUID payoutId,
                          @Param("cutoff") LocalDateTime cutoff);

    // Mark entries settled using the payout linkage set at reservation time.
    // Scoped to the specific payout — never touches another payout's entries.
    @Modifying
    @Query("""
            UPDATE LedgerEntry le
            SET le.settled = true
            WHERE le.payoutId = :payoutId
              AND le.settled = false
            """)
    void markSettled(@Param("payoutId") UUID payoutId);

    // Release entries back to the unsettled pool when a payout is marked FAILED.
    // Clears payout_id so the next settlement run picks them up under the new cutoff.
    @Modifying
    @Query("""
            UPDATE LedgerEntry le
            SET le.payoutId = null
            WHERE le.payoutId = :payoutId
            """)
    void releaseReservation(@Param("payoutId") UUID payoutId);
}

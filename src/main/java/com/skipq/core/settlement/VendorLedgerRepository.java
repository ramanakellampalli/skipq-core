package com.skipq.core.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface VendorLedgerRepository extends JpaRepository<VendorLedger, UUID> {

    // Upsert: creates the row if missing, increments if present.
    // Defensive against a vendor_ledger row not existing (e.g. older vendors,
    // or a race between vendor creation and first order completion).
    @Modifying
    @Query(value = """
            INSERT INTO vendor_ledger (vendor_id, available_balance, updated_at)
            VALUES (:vendorId, :amount, now())
            ON CONFLICT (vendor_id) DO UPDATE
            SET available_balance = vendor_ledger.available_balance + EXCLUDED.available_balance,
                updated_at = now()
            """, nativeQuery = true)
    void upsertBalance(@Param("vendorId") UUID vendorId, @Param("amount") BigDecimal amount);
}

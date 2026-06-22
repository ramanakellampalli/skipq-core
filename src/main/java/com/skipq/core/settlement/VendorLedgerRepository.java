package com.skipq.core.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface VendorLedgerRepository extends JpaRepository<VendorLedger, UUID> {

    @Modifying
    @Query("""
            UPDATE VendorLedger vl
            SET vl.availableBalance = vl.availableBalance + :amount,
                vl.updatedAt = CURRENT_TIMESTAMP
            WHERE vl.vendorId = :vendorId
            """)
    void incrementBalance(@Param("vendorId") UUID vendorId, @Param("amount") BigDecimal amount);
}

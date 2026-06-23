package com.skipq.core.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VendorPayoutRepository extends JpaRepository<VendorPayout, UUID> {

    @Query("SELECT vp FROM VendorPayout vp JOIN FETCH vp.vendor ORDER BY vp.createdAt DESC")
    List<VendorPayout> findAllWithVendor();

    @Query("SELECT vp FROM VendorPayout vp JOIN FETCH vp.vendor WHERE vp.status = :status ORDER BY vp.createdAt DESC")
    List<VendorPayout> findByStatusWithVendor(PayoutStatus status);
}

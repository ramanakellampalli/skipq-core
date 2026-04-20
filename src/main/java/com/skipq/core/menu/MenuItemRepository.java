package com.skipq.core.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    @Query("SELECT m FROM MenuItem m LEFT JOIN FETCH m.variants WHERE m.vendor.id = :vendorId ORDER BY m.displayOrder ASC")
    List<MenuItem> findAllByVendorIdWithVariants(@Param("vendorId") UUID vendorId);

    @Query("SELECT m FROM MenuItem m LEFT JOIN FETCH m.variants v WHERE m.vendor.id = :vendorId AND m.isAvailable = true AND v.isAvailable = true ORDER BY m.displayOrder ASC")
    List<MenuItem> findAvailableByVendorIdWithVariants(@Param("vendorId") UUID vendorId);

    @Query("SELECT m FROM MenuItem m WHERE m.id = :id AND m.vendor.id = :vendorId")
    Optional<MenuItem> findByIdAndVendorId(@Param("id") UUID id, @Param("vendorId") UUID vendorId);

    void deleteAllByVendorId(UUID vendorId);
}

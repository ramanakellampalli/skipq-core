package com.skipq.core.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    @Query("SELECT c FROM MenuCategory c WHERE c.vendor.id = :vendorId ORDER BY c.displayOrder ASC")
    List<MenuCategory> findAllByVendorIdOrdered(@Param("vendorId") UUID vendorId);

    Optional<MenuCategory> findByIdAndVendorId(UUID id, UUID vendorId);
}

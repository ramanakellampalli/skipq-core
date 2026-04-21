package com.skipq.core.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MenuVariantRepository extends JpaRepository<MenuVariant, UUID> {

    @Query("SELECT v FROM MenuVariant v WHERE v.id = :variantId AND v.menuItem.vendor.id = :vendorId")
    Optional<MenuVariant> findByIdAndVendorId(@Param("variantId") UUID variantId, @Param("vendorId") UUID vendorId);
}

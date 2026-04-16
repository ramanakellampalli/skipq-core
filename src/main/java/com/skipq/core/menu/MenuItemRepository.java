package com.skipq.core.menu;

import com.skipq.core.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findAllByVendorId(UUID vendorId);

    List<MenuItem> findAllByVendorIdAndIsAvailableTrue(UUID vendorId);
}

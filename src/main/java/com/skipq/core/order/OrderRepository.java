package com.skipq.core.order;

import com.skipq.core.common.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByVendorId(UUID vendorId);

    List<Order> findAllByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.menuItem WHERE o.vendor.id = :vendorId ORDER BY o.createdAt DESC")
    List<Order> findAllByVendorIdWithItems(@Param("vendorId") UUID vendorId);
}

package com.skipq.core.repository;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByVendorId(UUID vendorId);

    List<Order> findAllByVendorIdAndStatus(UUID vendorId, OrderStatus status);
}

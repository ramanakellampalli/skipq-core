package com.skipq.core.repository;

import com.skipq.core.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderId(UUID orderId);
}

package com.skipq.core.order;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.order.dto.OrderStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByVendorId(UUID vendorId);

    List<Order> findAllByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.vendor v JOIN FETCH o.items i JOIN FETCH i.menuItem WHERE v.user.email = :email ORDER BY o.createdAt DESC")
    List<Order> findAllByVendorEmailWithItems(@Param("email") String email);

    @Query("""
        SELECT
          COUNT(o) AS totalOrders,
          COALESCE(SUM(o.totalAmount), 0) AS revenue,
          SUM(CASE WHEN o.status IN ('ACCEPTED', 'PREPARING') THEN 1 ELSE 0 END) AS inProgress
        FROM Order o
        WHERE CAST(o.createdAt AS date) = CURRENT_DATE
        """)
    OrderStatsProjection getTodayStats();

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.vendor JOIN FETCH o.items i JOIN FETCH i.menuItem ORDER BY o.createdAt DESC")
    List<Order> findAllWithItems();

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.vendor JOIN FETCH o.items i JOIN FETCH i.menuItem WHERE o.user.email = :email ORDER BY o.createdAt DESC")
    List<Order> findAllByUserEmailWithItems(@Param("email") String email);
}

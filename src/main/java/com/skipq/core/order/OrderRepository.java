package com.skipq.core.order;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.order.dto.OrderStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.vendor
        JOIN FETCH o.items i
        JOIN FETCH i.menuItem
        LEFT JOIN FETCH i.variant
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.vendor
        JOIN FETCH o.items i
        JOIN FETCH i.menuItem
        LEFT JOIN FETCH i.variant
        WHERE o.razorpayOrderId = :razorpayOrderId
        """)
    Optional<Order> findByRazorpayOrderIdWithItems(@Param("razorpayOrderId") String razorpayOrderId);

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.vendor
        JOIN FETCH o.items i
        JOIN FETCH i.menuItem
        LEFT JOIN FETCH i.variant
        WHERE o.user.id = :userId
        ORDER BY o.createdAt DESC
        """)
    List<Order> findAllByUserIdWithItems(@Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.vendor v
        JOIN FETCH o.items i
        JOIN FETCH i.menuItem
        LEFT JOIN FETCH i.variant
        WHERE v.user.id = :userId
        ORDER BY o.createdAt DESC
        """)
    List<Order> findAllByVendorUserIdWithItems(@Param("userId") UUID userId);

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByVendorId(UUID vendorId);

    List<Order> findAllByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    @Query("""
        SELECT
          COUNT(o) AS totalOrders,
          COALESCE(SUM(o.totalAmount), 0) AS revenue,
          COALESCE(SUM(CASE WHEN o.status IN ('ACCEPTED', 'PREPARING') THEN 1 ELSE 0 END), 0) AS inProgress
        FROM Order o
        WHERE CAST(o.createdAt AS date) = CURRENT_DATE
        """)
    OrderStatsProjection getTodayStats();

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.vendor
        JOIN FETCH o.items i
        JOIN FETCH i.menuItem
        LEFT JOIN FETCH i.variant
        ORDER BY o.createdAt DESC
        """)
    List<Order> findAllWithItems();

    @Modifying
    @Query("DELETE FROM Order o WHERE o.status = com.skipq.core.common.OrderStatus.AWAITING_PAYMENT AND o.createdAt < :cutoff")
    int deleteStaleAwaitingPaymentOrders(@Param("cutoff") LocalDateTime cutoff);
}

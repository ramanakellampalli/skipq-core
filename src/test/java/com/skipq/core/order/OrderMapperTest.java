package com.skipq.core.order;

import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.OrderType;
import com.skipq.core.common.PaymentStatus;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuVariant;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.vendor.Vendor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    private Vendor vendor() {
        Vendor v = new Vendor();
        v.setId(UUID.randomUUID());
        v.setName("Test Vendor");
        return v;
    }

    private MenuItem menuItem(UUID id, String name) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName(name);
        m.setPrice(new BigDecimal("100.00"));
        return m;
    }

    private Order baseOrder(Vendor vendor) {
        return Order.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PAID)
                .orderType(OrderType.IMMEDIATE)
                .subtotal(new BigDecimal("100.00"))
                .cgst(new BigDecimal("2.50"))
                .sgst(new BigDecimal("2.50"))
                .igst(BigDecimal.ZERO)
                .taxAmount(new BigDecimal("5.00"))
                .platformFee(new BigDecimal("3.00"))
                .totalServiceFee(new BigDecimal("3.00"))
                .totalAmount(new BigDecimal("108.00"))
                .estimatedReadyAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Test
    void toResponse_mapsVendorAndState() {
        Vendor vendor = vendor();
        Order order = baseOrder(vendor);
        order.setItems(List.of());

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.vendor().id()).isEqualTo(vendor.getId());
        assertThat(response.vendor().name()).isEqualTo("Test Vendor");
        assertThat(response.state().orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.state().paymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void toResponse_mapsPricingCorrectly() {
        Order order = baseOrder(vendor());
        order.setItems(List.of());

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.pricing().subtotal()).isEqualByComparingTo("100.00");
        assertThat(response.pricing().tax().cgst()).isEqualByComparingTo("2.50");
        assertThat(response.pricing().tax().sgst()).isEqualByComparingTo("2.50");
        assertThat(response.pricing().fees().platformFee()).isEqualByComparingTo("3.00");
        assertThat(response.pricing().totalAmount()).isEqualByComparingTo("108.00");
    }

    @Test
    void toResponse_mapsItemWithoutVariant() {
        UUID menuItemId = UUID.randomUUID();
        MenuItem item = menuItem(menuItemId, "Burger");

        OrderItem orderItem = OrderItem.builder()
                .menuItem(item)
                .variant(null)
                .variantLabel(null)
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        Order order = baseOrder(vendor());
        order.setItems(List.of(orderItem));

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).menuItemId()).isEqualTo(menuItemId);
        assertThat(response.items().get(0).variantId()).isNull();
        assertThat(response.items().get(0).name()).isEqualTo("Burger");
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("100.00");
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void toResponse_mapsItemWithVariant() {
        UUID menuItemId = UUID.randomUUID();
        UUID variantId  = UUID.randomUUID();
        MenuItem item = menuItem(menuItemId, "Coffee");

        MenuVariant variant = new MenuVariant();
        variant.setId(variantId);
        variant.setLabel("Large");
        variant.setPrice(new BigDecimal("120.00"));

        OrderItem orderItem = OrderItem.builder()
                .menuItem(item)
                .variant(variant)
                .variantLabel("Large")
                .quantity(1)
                .unitPrice(new BigDecimal("120.00"))
                .build();

        Order order = baseOrder(vendor());
        order.setItems(List.of(orderItem));

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.items().get(0).variantId()).isEqualTo(variantId);
        assertThat(response.items().get(0).variantLabel()).isEqualTo("Large");
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("120.00");
    }

    @Test
    void toResponse_scheduledOrder_mapsTimeline() {
        LocalDateTime pickupAt = LocalDateTime.now().plusHours(2);
        Order order = baseOrder(vendor());
        order.setOrderType(OrderType.SCHEDULED);
        order.setScheduledPickupAt(pickupAt);
        order.setItems(List.of());

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.timeline().orderType()).isEqualTo(OrderType.SCHEDULED);
        assertThat(response.timeline().scheduledPickupAt()).isEqualTo(pickupAt);
    }

    @Test
    void toResponse_withExplicitItemsList_usesProvidedList() {
        UUID menuItemId = UUID.randomUUID();
        MenuItem item = menuItem(menuItemId, "Pizza");

        OrderItem orderItem = OrderItem.builder()
                .menuItem(item)
                .variant(null)
                .variantLabel(null)
                .quantity(1)
                .unitPrice(new BigDecimal("200.00"))
                .build();

        Order order = baseOrder(vendor());
        // order.items is null — mapper should use the explicitly passed list
        order.setItems(null);

        OrderResponse response = mapper.toResponse(order, List.of(orderItem));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).menuItemId()).isEqualTo(menuItemId);
    }

    @Test
    void toResponse_emptyItems_returnsEmptyList() {
        Order order = baseOrder(vendor());
        order.setItems(List.of());

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.items()).isEmpty();
    }
}

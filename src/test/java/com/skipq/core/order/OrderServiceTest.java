package com.skipq.core.order;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.Campus;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.PaymentStatus;
import com.skipq.core.config.AblyService;
import com.skipq.core.config.FcmService;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuVariant;
import com.skipq.core.menu.MenuVariantRepository;
import com.skipq.core.order.dto.OrderItemRequest;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.order.dto.PlaceOrderRequest;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock UserRepository userRepository;
    @Mock VendorRepository vendorRepository;
    @Mock MenuItemRepository menuItemRepository;
    @Mock MenuVariantRepository menuVariantRepository;
    @Mock AblyService ablyService;
    @Mock FcmService fcmService;

    @InjectMocks OrderService orderService;

    private UUID userId;
    private UUID vendorId;
    private Campus campus;
    private User user;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        campus   = Campus.builder().id(UUID.randomUUID()).name("Test Campus").emailDomain("test.edu").build();
        user     = User.builder().id(userId).name("Student").email("s@test.edu").campus(campus).build();
        vendor   = Vendor.builder().id(vendorId).name("Test Stall").isOpen(true).prepTime(15)
                         .gstRegistered(false).campus(campus).build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MenuItem menuItem(UUID vendorRef, boolean available) {
        return MenuItem.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .name("Water")
                .isVeg(true)
                .isAvailable(available)
                .displayOrder(0)
                .price(BigDecimal.valueOf(20))
                .variants(new ArrayList<>())
                .build();
    }

    private MenuVariant variant(MenuItem item) {
        return MenuVariant.builder()
                .id(UUID.randomUUID())
                .menuItem(item)
                .label("Full")
                .price(BigDecimal.valueOf(150))
                .isAvailable(true)
                .displayOrder(0)
                .build();
    }

    private Order savedOrder() {
        return Order.builder()
                .id(UUID.randomUUID())
                .user(user)
                .vendor(vendor)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(BigDecimal.valueOf(20))
                .cgst(BigDecimal.ZERO)
                .sgst(BigDecimal.ZERO)
                .igst(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .platformFee(BigDecimal.valueOf(0.60))
                .totalServiceFee(BigDecimal.valueOf(0.60))
                .totalAmount(BigDecimal.valueOf(20.60))
                .build();
    }

    // ── placeOrder — simple item (no variantId) ───────────────────────────────

    @Test
    void placeOrder_simpleItemUsesItemPrice() {
        MenuItem item = menuItem(vendorId, true);
        Order order = savedOrder();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), null, 1)));

        OrderResponse response = orderService.placeOrder(userId, req);

        assertThat(response).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        OrderItem saved = itemsCaptor.getValue().get(0);
        assertThat(saved.getUnitPrice()).isEqualByComparingTo("20");
        assertThat(saved.getVariant()).isNull();
    }

    @Test
    void placeOrder_withVariantUsesVariantPrice() {
        MenuItem item = menuItem(vendorId, true);
        MenuVariant v = variant(item);
        Order order = savedOrder();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(menuVariantRepository.findById(v.getId())).thenReturn(Optional.of(v));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), v.getId(), 1)));

        orderService.placeOrder(userId, req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        OrderItem saved = itemsCaptor.getValue().get(0);
        assertThat(saved.getUnitPrice()).isEqualByComparingTo("150");
        assertThat(saved.getVariant()).isEqualTo(v);
        assertThat(saved.getVariantLabel()).isEqualTo("Full");
    }

    @Test
    void placeOrder_subtotalAndFeesCalculatedCorrectly() {
        MenuItem item = menuItem(vendorId, true);
        Order order = savedOrder();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), null, 2)));

        orderService.placeOrder(userId, req);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getSubtotal()).isEqualByComparingTo("40.00");
        assertThat(saved.getCgst()).isEqualByComparingTo("0.00");
        assertThat(saved.getPlatformFee()).isEqualByComparingTo("1.20");
    }

    @Test
    void placeOrder_gstAppliedForRegisteredVendor() {
        vendor.setGstRegistered(true);
        MenuItem item = menuItem(vendorId, true);
        Order order = savedOrder();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), null, 1)));

        orderService.placeOrder(userId, req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getCgst()).isEqualByComparingTo("0.50");
        assertThat(saved.getSgst()).isEqualByComparingTo("0.50");
    }

    // ── placeOrder — error paths ───────────────────────────────────────────────

    @Test
    void placeOrder_throwsWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void placeOrder_throwsWhenVendorNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor not found");
    }

    @Test
    void placeOrder_throwsWhenVendorClosed() {
        vendor.setOpen(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void placeOrder_throwsWhenMenuItemNotFound() {
        UUID itemId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.empty());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(itemId, null, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Menu item not found");
    }

    @Test
    void placeOrder_throwsWhenItemUnavailable() {
        MenuItem item = menuItem(vendorId, false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), null, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void placeOrder_throwsWhenVariantNotFound() {
        MenuItem item = menuItem(vendorId, true);
        UUID badVariantId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(menuVariantRepository.findById(badVariantId)).thenReturn(Optional.empty());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), badVariantId, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Variant not found");
    }

    @Test
    void placeOrder_throwsWhenItemNotFromVendor() {
        Vendor otherVendor = Vendor.builder().id(UUID.randomUUID()).name("Other").isOpen(true)
                                   .campus(campus).build();
        MenuItem item = MenuItem.builder()
                .id(UUID.randomUUID()).vendor(otherVendor).name("Biriyani")
                .isVeg(false).isAvailable(true).displayOrder(0)
                .price(BigDecimal.valueOf(150)).variants(new ArrayList<>()).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), null, 1)));

        assertThatThrownBy(() -> orderService.placeOrder(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to this vendor");
    }

    @Test
    void placeOrder_publishesToAblyOnSuccess() {
        MenuItem item = menuItem(vendorId, true);
        Order order = savedOrder();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of());

        PlaceOrderRequest req = new PlaceOrderRequest(vendorId,
                List.of(new OrderItemRequest(item.getId(), null, 1)));

        orderService.placeOrder(userId, req);

        verify(ablyService).publish(eq("vendor:" + vendorId), eq("order"), any());
    }
}

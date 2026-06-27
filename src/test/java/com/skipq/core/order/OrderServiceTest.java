package com.skipq.core.order;

import com.razorpay.RazorpayException;
import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.Campus;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.common.OrderType;
import com.skipq.core.common.PaymentStatus;
import com.skipq.core.config.AblyService;
import com.skipq.core.config.FcmService;
import com.skipq.core.config.RazorpayService;
import com.skipq.core.discount.Discount;
import com.skipq.core.discount.DiscountScope;
import com.skipq.core.discount.DiscountType;
import com.skipq.core.discount.PriceResolver;
import com.skipq.core.discount.ResolvedPrice;
import com.skipq.core.menu.MenuItem;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuVariant;
import com.skipq.core.menu.MenuVariantRepository;
import com.skipq.core.order.dto.*;
import com.skipq.core.vendor.Vendor;
import com.skipq.core.settlement.LedgerService;
import com.skipq.core.vendor.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
    @Mock RazorpayService razorpayService;
    @Mock LedgerService ledgerService;
    @Mock PriceResolver priceResolver;
    @Spy  OrderMapper orderMapper;
    @Spy  OrderTransitionPolicy transitionPolicy;

    @InjectMocks OrderService orderService;

    private UUID userId;
    private UUID vendorId;
    private Campus campus;
    private User user;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(orderService, "schedulingWindowStart", "10:00");
        ReflectionTestUtils.setField(orderService, "schedulingWindowEnd", "17:00");
        ReflectionTestUtils.setField(orderService, "minLeadMinutes", 30);

        userId   = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        campus   = Campus.builder().id(UUID.randomUUID()).name("Test Campus").emailDomain("test.edu").build();
        user     = User.builder().id(userId).name("Student").email("s@test.edu").campus(campus).build();
        vendor   = Vendor.builder().id(vendorId).name("Test Stall").isOpen(true).prepTime(15)
                         .gstRegistered(false).campus(campus).build();

        // Default: no discount active — returns price unchanged. lenient() because many tests
        // don't call placeOrder and would otherwise trigger UnnecessaryStubbingException.
        lenient().when(priceResolver.resolve(any(MenuItem.class), any(BigDecimal.class), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    BigDecimal price = inv.getArgument(1);
                    return new ResolvedPrice(price, price, BigDecimal.ZERO, null);
                });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MenuItem menuItem(boolean available) {
        return MenuItem.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .name("Water")
                .isVeg(true)
                .isAvailable(available)
                .displayOrder(0)
                .price(BigDecimal.valueOf(20))
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

    private Order buildOrder(OrderStatus status, PaymentStatus paymentStatus) {
        return Order.builder()
                .id(UUID.randomUUID())
                .user(user)
                .vendor(vendor)
                .status(status)
                .paymentStatus(paymentStatus)
                .subtotal(new BigDecimal("100.00"))
                .cgst(BigDecimal.ZERO)
                .sgst(BigDecimal.ZERO)
                .igst(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .platformFee(new BigDecimal("3.00"))
                .totalServiceFee(new BigDecimal("3.00"))
                .totalAmount(new BigDecimal("103.00"))
                .estimatedReadyAt(LocalDateTime.now().plusMinutes(10))
                .items(new ArrayList<>())
                .build();
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Test
    void placeOrder_simpleItem_returnsRazorpayDetails() throws Exception {
        MenuItem item = menuItem(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenReturn("order_rzp123");

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        PlaceOrderResponse response = orderService.placeOrder(userId, request);

        assertThat(response.razorpayOrderId()).isEqualTo("order_rzp123");
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_key");
        assertThat(response.orderId()).isNotNull();
        // ₹20 item, 3% platform fee = ₹0.60, total ₹20.60 → 2060 paise
        assertThat(response.razorpayAmountPaise()).isEqualTo(2060L);
    }

    @Test
    void placeOrder_withVariant_usesVariantPrice() throws Exception {
        MenuItem item = menuItem(true);
        MenuVariant v = variant(item);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(menuVariantRepository.findById(v.getId())).thenReturn(Optional.of(v));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenReturn("order_rzp456");

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), v.getId(), 1)), null);
        PlaceOrderResponse response = orderService.placeOrder(userId, request);

        // ₹150 variant, 3% platform fee = ₹4.50, total ₹154.50 → 15450 paise
        assertThat(response.razorpayAmountPaise()).isEqualTo(15450L);
    }

    @Test
    void placeOrder_savesAsAwaitingPaymentAndDoesNotPublishAbly() throws Exception {
        MenuItem item = menuItem(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenReturn("order_rzp123");

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        orderService.placeOrder(userId, request);

        // razorpayOrderId is set on the managed entity after save — verify status/payment at save time
        verify(orderRepository).save(argThat(o ->
                o.getStatus() == OrderStatus.AWAITING_PAYMENT
                && o.getPaymentStatus() == PaymentStatus.PENDING));
        verify(ablyService, never()).publish(any(), any(), any());
    }

    @Test
    void placeOrder_razorpayFails_throwsServiceUnavailable() throws Exception {
        MenuItem item = menuItem(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenThrow(new RazorpayException("network error"));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        // In production @Transactional rolls back the insert; the unit test verifies the exception only
    }

    @Test
    void placeOrder_vendorClosed_throwsConflict() {
        vendor.setOpen(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verifyNoInteractions(razorpayService, orderRepository);
    }

    @Test
    void placeOrder_menuItemNotFound_throwsNotFound() {
        UUID itemId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(itemId)).thenReturn(Optional.empty());

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(itemId, null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void placeOrder_itemUnavailable_throwsConflict() {
        MenuItem item = menuItem(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void placeOrder_variantNotFound_throwsNotFound() {
        MenuItem item = menuItem(true);
        UUID badVariantId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(menuVariantRepository.findById(badVariantId)).thenReturn(Optional.empty());

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), badVariantId, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void placeOrder_itemFromDifferentVendor_throwsBadRequest() {
        Vendor other = Vendor.builder().id(UUID.randomUUID()).name("Other").build();
        MenuItem item = MenuItem.builder()
                .id(UUID.randomUUID()).vendor(other).name("Biriyani")
                .isVeg(false).isAvailable(true).displayOrder(0)
                .price(BigDecimal.valueOf(150)).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void placeOrder_ownStore_throwsForbidden() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void placeOrder_generalUser_campusVendor_throwsForbidden() {
        User generalUser = User.builder().id(userId).name("General").email("g@gmail.com").campus(null).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(generalUser));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void placeOrder_campusUser_generalVendor_succeeds() throws Exception {
        Vendor generalVendor = Vendor.builder().id(vendorId).name("City Cafe").isOpen(true).prepTime(15)
                .campus(null).gstRegistered(false).build();
        MenuItem item = MenuItem.builder().id(UUID.randomUUID()).vendor(generalVendor).name("Tea")
                .isVeg(true).isAvailable(true).displayOrder(0).price(BigDecimal.valueOf(20)).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(generalVendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenReturn("order_rzp_gen");

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        PlaceOrderResponse response = orderService.placeOrder(userId, request);

        assertThat(response.razorpayOrderId()).isEqualTo("order_rzp_gen");
    }

    // ── confirmPayment ────────────────────────────────────────────────────────

    @Test
    void confirmPayment_setsOrderPaidAndPublishesToVendor() {
        Order order = buildOrder(OrderStatus.AWAITING_PAYMENT, PaymentStatus.PENDING);
        when(orderRepository.findByRazorpayOrderIdWithItems("order_rzp123")).thenReturn(Optional.of(order));

        orderService.confirmPayment("order_rzp123", "pay_abc456");

        assertThat(order.getPaymentRef()).isEqualTo("pay_abc456");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(order);
        verify(ablyService).publish(eq("vendor:" + vendorId), eq("order"), any());
    }

    @Test
    void confirmPayment_orderNotFound_throwsIllegalArgument() {
        when(orderRepository.findByRazorpayOrderIdWithItems("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmPayment("unknown", "pay_xyz"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(ablyService);
    }

    // ── handlePaymentFailed ───────────────────────────────────────────────────

    @Test
    void handlePaymentFailed_orderFound_deletesItemsAndOrder() {
        Order order = buildOrder(OrderStatus.AWAITING_PAYMENT, PaymentStatus.PENDING);
        when(orderRepository.findByRazorpayOrderIdWithItems("order_rzp123")).thenReturn(Optional.of(order));

        orderService.handlePaymentFailed("order_rzp123");

        verify(orderItemRepository).deleteAllByOrderIn(List.of(order));
        verify(orderRepository).delete(order);
    }

    @Test
    void handlePaymentFailed_orderNotFound_noOp() {
        when(orderRepository.findByRazorpayOrderIdWithItems("unknown")).thenReturn(Optional.empty());

        orderService.handlePaymentFailed("unknown");

        verify(orderItemRepository, never()).deleteAllByOrderIn(any());
        verify(orderRepository, never()).delete(any());
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Test
    void cancelOrder_whileAwaitingPayment_deletesWithoutRefund() {
        Order order = buildOrder(OrderStatus.AWAITING_PAYMENT, PaymentStatus.PENDING);
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.cancelOrder(userId, order.getId());

        verify(orderItemRepository).deleteAllByOrderIn(List.of(order));
        verify(orderRepository).delete(order);
        verifyNoInteractions(razorpayService, ablyService, fcmService);
    }

    @Test
    void cancelOrder_whilePending_refundsAndPublishesAbly() throws Exception {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        order.setPaymentRef("pay_abc456");
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.cancelOrder(userId, order.getId());

        verify(razorpayService).refund(eq("pay_abc456"), anyLong());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(orderRepository).save(order);
        verify(ablyService).publish(eq("order:" + order.getId()), eq("status"), any());
        verify(fcmService).sendToUser(eq(user), anyString(), anyString());
    }

    @Test
    void cancelOrder_refundFails_throwsServiceUnavailable() throws Exception {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        order.setPaymentRef("pay_abc456");
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));
        doThrow(new RazorpayException("refund error")).when(razorpayService).refund(anyString(), anyLong());

        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_afterAccepted_throwsConflict() {
        Order order = buildOrder(OrderStatus.ACCEPTED, PaymentStatus.PAID);
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void cancelOrder_wrongUser_throwsNotFound() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(UUID.randomUUID(), order.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── getOrder ─────────────────────────────────────────────────────────────

    @Test
    void getOrder_happyPath_returnsResponse() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(userId, order.getId());

        assertThat(response.id()).isEqualTo(order.getId());
    }

    @Test
    void getOrder_notFound_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findByIdWithItems(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(userId, unknownId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getOrder_wrongUser_throwsNotFound() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(UUID.randomUUID(), order.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── getMyOrders ───────────────────────────────────────────────────────────

    @Test
    void getMyOrders_returnsMappedList() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(orderRepository.findAllByUserIdWithItems(userId)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getMyOrders(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(order.getId());
    }

    // ── getVendorOrders ───────────────────────────────────────────────────────

    @Test
    void getVendorOrders_returnsMappedList() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(orderRepository.findAllByVendorUserIdWithItems(userId)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getVendorOrders(userId);

        assertThat(result).hasSize(1);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_vendorAccepts_publishesAblyAndFcm() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(userId, order.getId(), OrderStatus.ACCEPTED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(response.state().orderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(ablyService).publish(eq("vendor:" + vendorId), eq("order"), any());
        verify(ablyService).publish(eq("order:" + order.getId()), eq("status"), any());
        verify(fcmService).sendToUser(eq(user), anyString(), anyString());
    }

    @Test
    void updateStatus_wrongVendor_throwsForbidden() {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        Vendor otherVendor = Vendor.builder().id(UUID.randomUUID()).name("Other").build();
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(otherVendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(userId, order.getId(), OrderStatus.ACCEPTED))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_toCompleted_creditsVendorLedger() {
        Order order = buildOrder(OrderStatus.READY, PaymentStatus.PAID);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.updateStatus(userId, order.getId(), OrderStatus.COMPLETED);

        verify(ledgerService).creditVendor(order);
    }

    @Test
    void updateStatus_toReady_doesNotCreditLedger() {
        Order order = buildOrder(OrderStatus.PREPARING, PaymentStatus.PAID);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.updateStatus(userId, order.getId(), OrderStatus.READY);

        verifyNoInteractions(ledgerService);
    }

    @Test
    void updateStatus_toRejected_firesRefund() throws Exception {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        order.setPaymentRef("pay_rej123");
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.updateStatus(userId, order.getId(), OrderStatus.REJECTED);

        // totalAmount is 103 → 10300 paise
        verify(razorpayService).refund("pay_rej123", 10300L);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_toRejected_noPaymentRef_skipsRefund() throws Exception {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        // paymentRef is null — order was never paid (edge case)
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.updateStatus(userId, order.getId(), OrderStatus.REJECTED);

        verifyNoInteractions(razorpayService);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void updateStatus_toRejected_refundFails_statusStillRejected() throws Exception {
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        order.setPaymentRef("pay_rej999");
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));
        doThrow(new RazorpayException("refund error"))
                .when(razorpayService).refund(anyString(), anyLong());

        // Refund failure must NOT propagate — rejection goes through, payment status stays PAID
        orderService.updateStatus(userId, order.getId(), OrderStatus.REJECTED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository).save(order);
    }

    // ── scheduled orders — placeOrder ─────────────────────────────────────────

    private Order buildScheduledOrder(LocalDateTime pickupAt) {
        Order order = buildOrder(OrderStatus.SCHEDULED, PaymentStatus.PAID);
        order.setOrderType(OrderType.SCHEDULED);
        order.setScheduledPickupAt(pickupAt);
        order.setPaymentRef("pay_sched123");
        return order;
    }

    @Test
    void placeOrder_validScheduled_setsScheduledTypeAndPickupAt() throws Exception {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        MenuItem item = menuItem(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenReturn("order_rzp_sched");

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), pickup);
        orderService.placeOrder(userId, request);

        verify(orderRepository).save(argThat(o ->
                o.getOrderType() == OrderType.SCHEDULED &&
                pickup.equals(o.getScheduledPickupAt())));
    }

    @Test
    void placeOrder_scheduledBeforeWindowStart_throwsBadRequest() {
        // 9 AM is before the 10 AM window start
        LocalDateTime beforeWindow = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)), beforeWindow);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(razorpayService, orderRepository);
    }

    @Test
    void placeOrder_scheduledAfterWindowEnd_throwsBadRequest() {
        // 6 PM is after the 5 PM window end
        LocalDateTime afterWindow = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)), afterWindow);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(razorpayService, orderRepository);
    }

    @Test
    void placeOrder_scheduledTooSoon_throwsBadRequest() {
        // Widen the window so only the lead-time check can fail
        ReflectionTestUtils.setField(orderService, "schedulingWindowStart", "00:00");
        ReflectionTestUtils.setField(orderService, "schedulingWindowEnd", "23:59");

        LocalDateTime tooSoon = LocalDateTime.now().plusMinutes(10);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(UUID.randomUUID(), null, 1)), tooSoon);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(razorpayService, orderRepository);
    }

    // ── scheduled orders — confirmPayment ─────────────────────────────────────

    @Test
    void confirmPayment_scheduledOrder_staysScheduledAndNotifiesCustomerOnly() {
        LocalDateTime pickup = LocalDateTime.now().plusHours(3);
        Order order = buildScheduledOrder(pickup);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        when(orderRepository.findByRazorpayOrderIdWithItems("order_sched")).thenReturn(Optional.of(order));

        orderService.confirmPayment("order_sched", "pay_sched456");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SCHEDULED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        // Customer channel gets update, vendor channel must NOT
        verify(ablyService).publish(eq("order:" + order.getId()), eq("status"), any());
        verify(ablyService, never()).publish(eq("vendor:" + vendorId), any(), any());
    }

    // ── scheduled orders — dispatchScheduledOrder ─────────────────────────────

    @Test
    void dispatchScheduledOrder_transitionsToPendingAndPublishesToBothChannels() {
        Order order = buildScheduledOrder(LocalDateTime.now().plusMinutes(14));

        orderService.dispatchScheduledOrder(order);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(order);
        verify(ablyService).publish(eq("vendor:" + vendorId), eq("order"), any());
        verify(ablyService).publish(eq("order:" + order.getId()), eq("status"), any());
    }

    // ── scheduled orders — cancelOrder ────────────────────────────────────────

    @Test
    void cancelOrder_whileScheduled_refundsAndCancels() throws Exception {
        Order order = buildScheduledOrder(LocalDateTime.now().plusHours(2));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        orderService.cancelOrder(userId, order.getId());

        verify(razorpayService).refund(eq("pay_sched123"), anyLong());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(orderRepository).save(order);
        verify(ablyService).publish(eq("order:" + order.getId()), eq("status"), any());
    }

    // ── variant cross-item validation ─────────────────────────────────────────

    @Test
    void placeOrder_variantFromDifferentItem_throwsBadRequest() {
        MenuItem itemA = menuItem(true);
        MenuItem itemB = menuItem(true);
        MenuVariant variantOfB = variant(itemB); // variant belongs to itemB, not itemA

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(itemA.getId())).thenReturn(Optional.of(itemA));
        when(menuVariantRepository.findById(variantOfB.getId())).thenReturn(Optional.of(variantOfB));

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(itemA.getId(), variantOfB.getId(), 1)), null);
        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ── order state machine ───────────────────────────────────────────────────

    @Test
    void updateStatus_invalidTransition_throwsConflict() {
        // PENDING → READY is not a valid transition (must go PENDING → ACCEPTED → PREPARING → READY)
        Order order = buildOrder(OrderStatus.PENDING, PaymentStatus.PAID);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(userId, order.getId(), OrderStatus.READY))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_terminalState_throwsConflict() {
        // COMPLETED → REJECTED must be blocked to prevent refund abuse
        Order order = buildOrder(OrderStatus.COMPLETED, PaymentStatus.PAID);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(orderRepository.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(userId, order.getId(), OrderStatus.REJECTED))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(razorpayService);
    }

    // ── discount integration ──────────────────────────────────────────────────

    @Test
    void placeOrder_itemWithActiveDiscount_freezesDiscountPricesOnOrderItem() throws Exception {
        MenuItem item     = menuItem(true);
        Discount discount = Discount.builder()
                .id(UUID.randomUUID())
                .vendor(vendor)
                .name("10% off")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .scope(DiscountScope.ITEM)
                .active(true)
                .priority(0)
                .build();

        // Override default stub: 10% discount on ₹20 item → ₹18 discounted, ₹2 off
        when(priceResolver.resolve(eq(item), eq(BigDecimal.valueOf(20)), any(LocalDateTime.class)))
                .thenReturn(new ResolvedPrice(
                        new BigDecimal("20.00"),
                        new BigDecimal("18.00"),
                        new BigDecimal("2.00"),
                        discount
                ));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(razorpayService.createOrder(anyLong(), anyString())).thenReturn("order_disc123");

        var request = new PlaceOrderRequest(vendorId, List.of(new OrderItemRequest(item.getId(), null, 1)), null);
        PlaceOrderResponse response = orderService.placeOrder(userId, request);

        // ₹18 discounted price, 3% platform fee = ₹0.54, total ₹18.54 → 1854 paise
        assertThat(response.razorpayAmountPaise()).isEqualTo(1854L);

        verify(orderItemRepository).saveAll(argThat(items -> {
            OrderItem saved = ((List<OrderItem>) items).get(0);
            return saved.getUnitPrice().compareTo(new BigDecimal("18.00")) == 0
                    && saved.getOriginalPrice().compareTo(new BigDecimal("20.00")) == 0
                    && saved.getDiscountAmount().compareTo(new BigDecimal("2.00")) == 0
                    && saved.getDiscount() == discount;
        }));
    }
}

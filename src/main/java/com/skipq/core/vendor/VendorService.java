package com.skipq.core.vendor;

import com.skipq.core.auth.UserRepository;
import com.skipq.core.menu.MenuCategoryRepository;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuItemService;
import com.skipq.core.menu.dto.MenuCategoryResponse;
import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.order.Order;
import com.skipq.core.order.OrderItemRepository;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.order.dto.OrderItemResponse;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.vendor.dto.UpdateVendorRequest;
import com.skipq.core.vendor.dto.VendorDashboardResponse;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemService menuItemService;
    private final UserRepository userRepository;

    public VendorResponse getProfile(UUID userId) {
        return toResponse(findByUserId(userId));
    }

    @Transactional
    public VendorResponse updateProfile(UUID userId, UpdateVendorRequest request) {
        Vendor vendor = findByUserId(userId);
        if (request.isOpen() != null) vendor.setOpen(request.isOpen());
        if (request.prepTime() != null) vendor.setPrepTime(request.prepTime());
        return toResponse(vendorRepository.save(vendor));
    }

    @Transactional(readOnly = true)
    public VendorDashboardResponse sync(UUID userId) {
        List<Order> orders = orderRepository.findAllByVendorUserIdWithItems(userId);

        Vendor vendor = orders.isEmpty()
                ? findByUserId(userId)
                : orders.get(0).getVendor();

        List<OrderResponse> allOrders = orders.stream().map(order -> {
            List<OrderItemResponse> itemResponses = order.getItems().stream()
                    .map(i -> new OrderItemResponse(
                            i.getMenuItem().getId(),
                            i.getVariant() != null ? i.getVariant().getId() : null,
                            i.getMenuItem().getName(),
                            i.getVariantLabel(),
                            i.getQuantity(),
                            i.getUnitPrice(),
                            i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))
                    ))
                    .toList();

            var vendorInfo = new OrderResponse.VendorInfo(order.getVendor().getId(), order.getVendor().getName());
            var state      = new OrderResponse.OrderState(order.getStatus(), order.getPaymentStatus());
            var tax        = new OrderResponse.TaxBreakdown(order.getCgst(), order.getSgst(), order.getIgst(), order.getTaxAmount());
            var fees       = new OrderResponse.Fees(order.getPlatformFee(), order.getPaymentTerminalFee(), order.getTotalServiceFee());
            var pricing    = new OrderResponse.Pricing(order.getSubtotal(), tax, fees, order.getTotalAmount());
            var timeline   = new OrderResponse.Timeline(order.getCreatedAt(), order.getEstimatedReadyAt());
            return new OrderResponse(order.getId(), vendorInfo, state, pricing, timeline, itemResponses);
        }).toList();

        List<OrderResponse> activeOrders = allOrders.stream()
                .filter(o -> o.state().orderStatus() != com.skipq.core.common.OrderStatus.COMPLETED
                          && o.state().orderStatus() != com.skipq.core.common.OrderStatus.REJECTED)
                .toList();
        List<OrderResponse> pastOrders = allOrders.stream()
                .filter(o -> o.state().orderStatus() == com.skipq.core.common.OrderStatus.COMPLETED
                          || o.state().orderStatus() == com.skipq.core.common.OrderStatus.REJECTED)
                .toList();

        // Menu: all items with variants, grouped into categories + uncategorized
        List<MenuItemResponse> allItems = menuItemRepository.findAllByVendorIdWithVariants(vendor.getId())
                .stream().map(menuItemService::toItemResponse).toList();

        List<MenuCategoryResponse> categories = categoryRepository.findAllByVendorIdOrdered(vendor.getId())
                .stream().map(c -> {
                    Set<UUID> categoryItemIds = c.getItems().stream()
                            .map(item -> item.getId()).collect(Collectors.toSet());
                    List<MenuItemResponse> items = allItems.stream()
                            .filter(i -> categoryItemIds.contains(i.id()))
                            .toList();
                    return new MenuCategoryResponse(c.getId(), c.getName(), c.getDisplayOrder(), items);
                }).toList();

        List<MenuItemResponse> uncategorized = allItems.stream()
                .filter(i -> i.categoryId() == null)
                .toList();

        return new VendorDashboardResponse(toResponse(vendor), activeOrders, pastOrders, categories, uncategorized);
    }

    public List<VendorResponse> getOpenVendors() {
        return vendorRepository.findAllByIsOpenTrue().stream().map(this::toResponse).toList();
    }

    public List<VendorResponse> getAllVendors() {
        return vendorRepository.findAllByOrderByIsOpenDesc().stream().map(this::toResponse).toList();
    }

    public List<VendorResponse> getVendorsByCampus(com.skipq.core.campus.Campus campus) {
        return vendorRepository.findAllByCampusOrderByIsOpenDesc(campus).stream().map(this::toResponse).toList();
    }

    public VendorResponse getById(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        Vendor vendor = findByUserId(userId);
        List<Order> orders = orderRepository.findAllByVendorId(vendor.getId());
        orderItemRepository.deleteAllByOrderIn(orders);
        orderRepository.deleteAll(orders);
        menuItemRepository.deleteAllByVendorId(vendor.getId());
        vendorRepository.delete(vendor);
        userRepository.deleteById(userId);
    }

    private Vendor findByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(vendor.getId(), vendor.getName(), vendor.isOpen(), vendor.getPrepTime(),
                vendor.getBusinessName(), vendor.isGstRegistered(), vendor.getGstin(), vendor.isKycApproved(),
                vendor.getCampus().getId(), vendor.getCampus().getName());
    }
}

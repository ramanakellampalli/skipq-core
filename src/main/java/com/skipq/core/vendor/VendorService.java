package com.skipq.core.vendor;

import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.order.OrderItem;
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

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    public VendorResponse getProfile(String email) {
        Vendor vendor = findByEmail(email);
        return toResponse(vendor);
    }

    @Transactional
    public VendorResponse updateProfile(String email, UpdateVendorRequest request) {
        Vendor vendor = findByEmail(email);

        if (request.isOpen() != null) {
            vendor.setOpen(request.isOpen());
        }
        if (request.prepTime() != null) {
            vendor.setPrepTime(request.prepTime());
        }

        return toResponse(vendorRepository.save(vendor));
    }

    @Transactional(readOnly = true)
    public VendorDashboardResponse sync(String email) {
        Vendor vendor = findByEmail(email);

        List<OrderResponse> allOrders = orderRepository.findAllByVendorIdWithItems(vendor.getId())
                .stream()
                .map(order -> {
                    List<OrderItemResponse> itemResponses = order.getItems().stream()
                            .map(i -> new OrderItemResponse(
                                    i.getMenuItem().getId(),
                                    i.getMenuItem().getName(),
                                    i.getQuantity(),
                                    i.getUnitPrice(),
                                    i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity()))
                            ))
                            .toList();
                    return new OrderResponse(
                            order.getId(),
                            vendor.getId(),
                            vendor.getName(),
                            order.getStatus(),
                            order.getPaymentStatus(),
                            order.getTotalAmount(),
                            order.getEstimatedReadyAt(),
                            order.getCreatedAt(),
                            itemResponses
                    );
                })
                .toList();

        List<OrderResponse> activeOrders = allOrders.stream()
                .filter(o -> o.status() != com.skipq.core.common.OrderStatus.COMPLETED
                          && o.status() != com.skipq.core.common.OrderStatus.REJECTED)
                .toList();
        List<OrderResponse> pastOrders = allOrders.stream()
                .filter(o -> o.status() == com.skipq.core.common.OrderStatus.COMPLETED
                          || o.status() == com.skipq.core.common.OrderStatus.REJECTED)
                .toList();

        List<MenuItemResponse> menuItems = menuItemRepository.findAllByVendorId(vendor.getId())
                .stream()
                .map(m -> new MenuItemResponse(m.getId(), m.getName(), m.getPrice(), m.isAvailable()))
                .toList();

        return new VendorDashboardResponse(toResponse(vendor), activeOrders, pastOrders, menuItems);
    }

    public List<VendorResponse> getOpenVendors() {
        return vendorRepository.findAllByIsOpenTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VendorResponse getById(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
    }

    private Vendor findByEmail(String email) {
        return vendorRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user"));
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(vendor.getId(), vendor.getName(), vendor.isOpen(), vendor.getPrepTime());
    }
}

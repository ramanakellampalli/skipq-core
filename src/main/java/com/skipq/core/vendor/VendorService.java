package com.skipq.core.vendor;

import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.AccountStatus;
import com.skipq.core.common.OrderStatus;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuItemService;
import com.skipq.core.menu.dto.MenuItemResponse;
import com.skipq.core.order.Order;
import com.skipq.core.order.OrderItemRepository;
import com.skipq.core.order.OrderMapper;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.order.dto.OrderResponse;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.support.dto.ServiceRequestResponse;
import com.skipq.core.vendor.dto.UpdateVendorRequest;
import com.skipq.core.vendor.dto.VendorDashboardResponse;
import com.skipq.core.vendor.dto.VendorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemService menuItemService;
    private final UserRepository userRepository;
    private final ServiceRequestService serviceRequestService;
    private final OrderMapper orderMapper;

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

        List<OrderResponse> allOrders = orders.stream().map(orderMapper::toResponse).toList();

        var activeStatuses = EnumSet.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY);
        var pastStatuses   = EnumSet.of(OrderStatus.COMPLETED, OrderStatus.REJECTED, OrderStatus.CANCELLED);

        List<OrderResponse> activeOrders = allOrders.stream()
                .filter(o -> activeStatuses.contains(o.state().orderStatus()))
                .toList();
        List<OrderResponse> pastOrders = allOrders.stream()
                .filter(o -> pastStatuses.contains(o.state().orderStatus()))
                .toList();

        List<MenuItemResponse> items = menuItemRepository.findAllByVendorIdWithVariants(vendor.getId())
                .stream().map(menuItemService::toItemResponse).toList();

        List<ServiceRequestResponse> serviceRequests = serviceRequestService.findByUser(userId);

        return new VendorDashboardResponse(toResponse(vendor), activeOrders, pastOrders, items, serviceRequests);
    }

    public List<VendorResponse> getOpenVendors() {
        return vendorRepository.findAllByIsOpenTrueAndAccountStatus(com.skipq.core.common.AccountStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    public List<VendorResponse> getAllVendors() {
        return vendorRepository.findAllByOrderByIsOpenDesc().stream().map(this::toResponse).toList();
    }

    public List<VendorResponse> getVendorsByCampus(com.skipq.core.campus.Campus campus) {
        var campusVendors = vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE);
        var generalVendors = vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE);
        return Stream.concat(campusVendors.stream(), generalVendors.stream())
                .map(this::toResponse).toList();
    }

    public List<VendorResponse> getGeneralVendors() {
        return vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
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
        var campus = vendor.getCampus();
        return new VendorResponse(vendor.getId(), vendor.getName(), vendor.isOpen(), vendor.getPrepTime(),
                vendor.getBusinessName(), vendor.isGstRegistered(), vendor.getGstin(), vendor.isKycApproved(),
                campus != null ? campus.getId() : null,
                campus != null ? campus.getName() : null,
                vendor.getAccountStatus(), vendor.getSuspensionNote(), vendor.getLogoUrl(),
                vendor.getCity(), vendor.getPhone());
    }
}

package com.skipq.core.vendor;

import com.skipq.core.campus.Campus;
import com.skipq.core.common.AccountStatus;
import com.skipq.core.menu.MenuItemRepository;
import com.skipq.core.menu.MenuItemService;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.order.OrderItemRepository;
import com.skipq.core.order.OrderMapper;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.vendor.dto.VendorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock VendorRepository vendorRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock MenuItemRepository menuItemRepository;
    @Mock MenuItemService menuItemService;
    @Mock UserRepository userRepository;
    @Mock ServiceRequestService serviceRequestService;
    @Mock OrderMapper orderMapper;

    @InjectMocks VendorService vendorService;

    private Campus campus;

    @BeforeEach
    void setUp() {
        campus = Campus.builder()
                .id(UUID.randomUUID())
                .name("Test Campus")
                .emailDomain("campus.edu")
                .build();
    }

    private Vendor campusVendor(String name, boolean open) {
        return Vendor.builder()
                .id(UUID.randomUUID())
                .campus(campus)
                .name(name)
                .isOpen(open)
                .prepTime(10)
                .accountStatus(AccountStatus.ACTIVE)
                .city(null)
                .phone("+91 90000 00001")
                .build();
    }

    private Vendor generalVendor(String name) {
        return Vendor.builder()
                .id(UUID.randomUUID())
                .campus(null)
                .name(name)
                .isOpen(true)
                .prepTime(15)
                .accountStatus(AccountStatus.ACTIVE)
                .city("Bangalore")
                .phone("+91 90000 00002")
                .build();
    }

    @Test
    void getVendorsByCampus_mergesCampusAndGeneralVendors() {
        Vendor cv = campusVendor("Campus Stall", true);
        Vendor gv = generalVendor("City Cafe");

        when(vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE)).thenReturn(List.of(cv));
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of(gv));

        List<VendorResponse> result = vendorService.getVendorsByCampus(campus);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(VendorResponse::name)).containsExactly("Campus Stall", "City Cafe");
    }

    @Test
    void getVendorsByCampus_noGeneralVendors_returnsCampusOnly() {
        Vendor cv = campusVendor("Campus Stall", true);

        when(vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE)).thenReturn(List.of(cv));
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of());

        List<VendorResponse> result = vendorService.getVendorsByCampus(campus);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Campus Stall");
    }

    @Test
    void getGeneralVendors_returnsNullCampusActiveVendors() {
        Vendor gv = generalVendor("City Cafe");

        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of(gv));

        List<VendorResponse> result = vendorService.getGeneralVendors();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("City Cafe");
        assertThat(result.get(0).campusId()).isNull();
        assertThat(result.get(0).campusName()).isNull();
    }

    @Test
    void getGeneralVendors_empty_returnsEmptyList() {
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(vendorService.getGeneralVendors()).isEmpty();
    }

    @Test
    void toResponse_withCampus_mapsCampusFields() {
        Vendor cv = campusVendor("Campus Stall", true);
        when(vendorRepository.findAllByCampusAndAccountStatusOrderByIsOpenDesc(campus, AccountStatus.ACTIVE)).thenReturn(List.of(cv));
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of());

        VendorResponse response = vendorService.getVendorsByCampus(campus).get(0);

        assertThat(response.campusId()).isEqualTo(campus.getId());
        assertThat(response.campusName()).isEqualTo("Test Campus");
    }

    @Test
    void toResponse_nullCampus_mapsCityAndPhone() {
        Vendor gv = generalVendor("City Cafe");
        when(vendorRepository.findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus.ACTIVE))
                .thenReturn(List.of(gv));

        VendorResponse response = vendorService.getGeneralVendors().get(0);

        assertThat(response.campusId()).isNull();
        assertThat(response.campusName()).isNull();
        assertThat(response.city()).isEqualTo("Bangalore");
        assertThat(response.phone()).isEqualTo("+91 90000 00002");
    }

    @Test
    void getById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(vendorRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> vendorService.getById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor not found");
    }
}

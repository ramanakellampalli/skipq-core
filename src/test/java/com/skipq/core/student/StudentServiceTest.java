package com.skipq.core.student;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.campus.Campus;
import com.skipq.core.common.UserRole;
import com.skipq.core.config.VendorImageService;
import com.skipq.core.menu.MenuItemService;
import com.skipq.core.order.OrderItemRepository;
import com.skipq.core.order.OrderMapper;
import com.skipq.core.order.OrderRepository;
import com.skipq.core.student.dto.StudentSyncResponse;
import com.skipq.core.support.ServiceRequestService;
import com.skipq.core.vendor.VendorService;
import com.skipq.core.vendor.dto.VendorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock MenuItemService menuItemService;
    @Mock VendorService vendorService;
    @Mock UserRepository userRepository;
    @Mock VendorImageService vendorImageService;
    @Mock ServiceRequestService serviceRequestService;
    @Mock OrderMapper orderMapper;

    @InjectMocks StudentService studentService;

    private Campus campus() {
        return Campus.builder()
                .id(UUID.randomUUID())
                .name("Test Campus")
                .emailDomain("campus.edu")
                .build();
    }

    private User studentWithCampus(Campus campus, String phone) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setName("Ramana");
        u.setEmail("ramana@campus.edu");
        u.setRole(UserRole.STUDENT);
        u.setCampus(campus);
        u.setPhone(phone);
        return u;
    }

    private User generalStudent(String phone) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setName("Priya");
        u.setEmail("priya@gmail.com");
        u.setRole(UserRole.STUDENT);
        u.setCampus(null);
        u.setPhone(phone);
        return u;
    }

    @Test
    void sync_campusUser_callsGetVendorsByCampus() {
        Campus c = campus();
        User student = studentWithCampus(c, "+91 98765 43210");
        UUID userId = student.getId();

        when(userRepository.findByIdWithCampus(userId)).thenReturn(Optional.of(student));
        when(vendorService.getVendorsByCampus(c)).thenReturn(List.of());
        when(orderRepository.findAllByUserIdWithItems(userId)).thenReturn(List.of());
        when(serviceRequestService.findByUser(userId)).thenReturn(List.of());

        StudentSyncResponse response = studentService.sync(userId);

        verify(vendorService).getVendorsByCampus(c);
        verify(vendorService, never()).getGeneralVendors();
        assertThat(response.profile().campusId()).isEqualTo(c.getId());
        assertThat(response.profile().campusName()).isEqualTo("Test Campus");
    }

    @Test
    void sync_generalUser_callsGetGeneralVendors() {
        User student = generalStudent("+91 91111 22222");
        UUID userId = student.getId();

        when(userRepository.findByIdWithCampus(userId)).thenReturn(Optional.of(student));
        when(vendorService.getGeneralVendors()).thenReturn(List.of());
        when(orderRepository.findAllByUserIdWithItems(userId)).thenReturn(List.of());
        when(serviceRequestService.findByUser(userId)).thenReturn(List.of());

        StudentSyncResponse response = studentService.sync(userId);

        verify(vendorService).getGeneralVendors();
        verify(vendorService, never()).getVendorsByCampus(any());
        assertThat(response.profile().campusId()).isNull();
        assertThat(response.profile().campusName()).isNull();
    }

    @Test
    void sync_includesPhoneInProfile() {
        User student = generalStudent("+91 91111 22222");
        UUID userId = student.getId();

        when(userRepository.findByIdWithCampus(userId)).thenReturn(Optional.of(student));
        when(vendorService.getGeneralVendors()).thenReturn(List.of());
        when(orderRepository.findAllByUserIdWithItems(userId)).thenReturn(List.of());
        when(serviceRequestService.findByUser(userId)).thenReturn(List.of());

        StudentSyncResponse response = studentService.sync(userId);

        assertThat(response.profile().phone()).isEqualTo("+91 91111 22222");
    }

    @Test
    void sync_phoneNull_profilePhoneIsNull() {
        User student = generalStudent(null);
        UUID userId = student.getId();

        when(userRepository.findByIdWithCampus(userId)).thenReturn(Optional.of(student));
        when(vendorService.getGeneralVendors()).thenReturn(List.of());
        when(orderRepository.findAllByUserIdWithItems(userId)).thenReturn(List.of());
        when(serviceRequestService.findByUser(userId)).thenReturn(List.of());

        StudentSyncResponse response = studentService.sync(userId);

        assertThat(response.profile().phone()).isNull();
    }

    @Test
    void sync_returnsVendorsFromService() {
        Campus c = campus();
        User student = studentWithCampus(c, null);
        UUID userId = student.getId();

        VendorResponse vendorResp = mock(VendorResponse.class);
        when(vendorResp.id()).thenReturn(UUID.randomUUID());
        when(userRepository.findByIdWithCampus(userId)).thenReturn(Optional.of(student));
        when(vendorService.getVendorsByCampus(c)).thenReturn(List.of(vendorResp));
        when(orderRepository.findAllByUserIdWithItems(userId)).thenReturn(List.of());
        when(serviceRequestService.findByUser(userId)).thenReturn(List.of());
        when(vendorImageService.getImagesForVendor(any())).thenReturn(List.of());

        StudentSyncResponse response = studentService.sync(userId);

        assertThat(response.vendors()).hasSize(1);
    }

    @Test
    void sync_studentNotFound_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdWithCampus(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.sync(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Student not found");
    }
}

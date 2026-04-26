package com.skipq.core.support;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.common.UserRole;
import com.skipq.core.support.dto.AdminServiceRequestResponse;
import com.skipq.core.support.dto.CreateServiceRequestRequest;
import com.skipq.core.support.dto.ServiceRequestResponse;
import com.skipq.core.support.dto.UpdateServiceRequestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock ServiceRequestRepository repository;
    @Mock UserRepository userRepository;
    @InjectMocks ServiceRequestService service;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@skipq.dev")
                .role(UserRole.STUDENT)
                .build();
    }

    // --- create ---

    @Test
    void create_savesAndReturnsUserResponse() {
        var request = new CreateServiceRequestRequest(ServiceRequestType.PAYMENT_ISSUE, "Charged twice");

        ServiceRequest saved = ServiceRequest.builder()
                .id(UUID.randomUUID())
                .user(user)
                .role(UserRole.STUDENT)
                .type(ServiceRequestType.PAYMENT_ISSUE)
                .description("Charged twice")
                .status(ServiceRequestStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any())).thenReturn(saved);

        ServiceRequestResponse response = service.create(userId, request);

        assertThat(response.type()).isEqualTo(ServiceRequestType.PAYMENT_ISSUE);
        assertThat(response.status()).isEqualTo(ServiceRequestStatus.OPEN);
        assertThat(response.adminResponse()).isNull();
        verify(repository).save(any(ServiceRequest.class));
    }

    @Test
    void create_throwsWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var request = new CreateServiceRequestRequest(ServiceRequestType.OTHER, "desc");
        assertThatThrownBy(() -> service.create(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(repository, never()).save(any());
    }

    // --- update ---

    @Test
    void update_setsStatusAndNotes() {
        UUID srId = UUID.randomUUID();
        ServiceRequest sr = ServiceRequest.builder()
                .id(srId).user(user).role(UserRole.STUDENT)
                .type(ServiceRequestType.TECHNICAL).description("Details")
                .status(ServiceRequestStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(srId)).thenReturn(Optional.of(sr));
        when(repository.save(sr)).thenReturn(sr);

        var req = new UpdateServiceRequestRequest(ServiceRequestStatus.IN_PROGRESS, null, "Internal note");
        AdminServiceRequestResponse response = service.update(srId, req);

        assertThat(response.status()).isEqualTo(ServiceRequestStatus.IN_PROGRESS);
        assertThat(response.adminNotes()).isEqualTo("Internal note");
        assertThat(sr.getAdminRespondedAt()).isNull();
    }

    @Test
    void update_setsAdminRespondedAtWhenResponseProvided() {
        UUID srId = UUID.randomUUID();
        ServiceRequest sr = ServiceRequest.builder()
                .id(srId).user(user).role(UserRole.VENDOR)
                .type(ServiceRequestType.PAYOUT_ISSUE).description("Details")
                .status(ServiceRequestStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(srId)).thenReturn(Optional.of(sr));
        when(repository.save(sr)).thenReturn(sr);

        var req = new UpdateServiceRequestRequest(ServiceRequestStatus.RESOLVED, "Payout sent", null);
        service.update(srId, req);

        assertThat(sr.getAdminResponse()).isEqualTo("Payout sent");
        assertThat(sr.getAdminRespondedAt()).isNotNull();
    }

    @Test
    void update_doesNotSetRespondedAtForBlankResponse() {
        UUID srId = UUID.randomUUID();
        ServiceRequest sr = ServiceRequest.builder()
                .id(srId).user(user).role(UserRole.STUDENT)
                .type(ServiceRequestType.OTHER).description("desc")
                .status(ServiceRequestStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(srId)).thenReturn(Optional.of(sr));
        when(repository.save(sr)).thenReturn(sr);

        service.update(srId, new UpdateServiceRequestRequest(ServiceRequestStatus.CLOSED, "  ", null));

        assertThat(sr.getAdminRespondedAt()).isNull();
    }

    @Test
    void update_throwsWhenNotFound() {
        UUID srId = UUID.randomUUID();
        when(repository.findById(srId)).thenReturn(Optional.empty());

        var updateReq = new UpdateServiceRequestRequest(ServiceRequestStatus.CLOSED, null, null);
        assertThatThrownBy(() -> service.update(srId, updateReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Service request not found");
    }

    // --- findByUser ---

    @Test
    void findByUser_returnsUserResponses() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(UUID.randomUUID()).user(user).role(UserRole.STUDENT)
                .type(ServiceRequestType.ACCOUNT_ISSUE).description("Login broken")
                .status(ServiceRequestStatus.OPEN).createdAt(LocalDateTime.now()).build();

        when(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(sr));

        List<ServiceRequestResponse> result = service.findByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(ServiceRequestType.ACCOUNT_ISSUE);
    }

    // --- findAll ---

    @Test
    void findAll_returnsAdminResponses() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(UUID.randomUUID()).user(user).role(UserRole.VENDOR)
                .type(ServiceRequestType.BILLING_ISSUE).description("Invoice wrong")
                .status(ServiceRequestStatus.OPEN).createdAt(LocalDateTime.now()).build();

        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sr));

        List<AdminServiceRequestResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userEmail()).isEqualTo("test@skipq.dev");
        assertThat(result.get(0).role()).isEqualTo(UserRole.VENDOR);
    }

    // --- static mappers ---

    @Test
    void toAdminResponse_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ServiceRequest sr = ServiceRequest.builder()
                .id(UUID.randomUUID()).user(user).role(UserRole.STUDENT)
                .type(ServiceRequestType.REFUND_ISSUE).description("Details")
                .status(ServiceRequestStatus.RESOLVED)
                .adminResponse("Refunded").adminNotes("Internal").adminRespondedAt(now)
                .createdAt(now).build();

        AdminServiceRequestResponse r = ServiceRequestService.toAdminResponse(sr);

        assertThat(r.userName()).isEqualTo("Test User");
        assertThat(r.userEmail()).isEqualTo("test@skipq.dev");
        assertThat(r.adminResponse()).isEqualTo("Refunded");
        assertThat(r.adminNotes()).isEqualTo("Internal");
        assertThat(r.adminRespondedAt()).isEqualTo(now);
    }
}

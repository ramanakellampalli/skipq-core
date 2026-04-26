package com.skipq.core.support;

import com.skipq.core.auth.User;
import com.skipq.core.auth.UserRepository;
import com.skipq.core.support.dto.AdminServiceRequestResponse;
import com.skipq.core.support.dto.CreateServiceRequestRequest;
import com.skipq.core.support.dto.ServiceRequestResponse;
import com.skipq.core.support.dto.UpdateServiceRequestRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public ServiceRequestResponse create(UUID userId, CreateServiceRequestRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ServiceRequest sr = ServiceRequest.builder()
                .user(user)
                .role(user.getRole())
                .type(request.type())
                .description(request.description())
                .build();

        return toUserResponse(repository.save(sr));
    }

    @Transactional
    public AdminServiceRequestResponse update(UUID id, UpdateServiceRequestRequest request) {
        ServiceRequest sr = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service request not found"));

        sr.setStatus(request.status());
        sr.setAdminNotes(request.adminNotes());

        if (request.adminResponse() != null && !request.adminResponse().isBlank()) {
            sr.setAdminResponse(request.adminResponse());
            sr.setAdminRespondedAt(LocalDateTime.now());
        }

        return toAdminResponse(repository.save(sr));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> findByUser(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(ServiceRequestService::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminServiceRequestResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream().map(ServiceRequestService::toAdminResponse).toList();
    }

    static ServiceRequestResponse toUserResponse(ServiceRequest sr) {
        return new ServiceRequestResponse(
                sr.getId(),
                sr.getType(),
                sr.getStatus(),
                sr.getAdminResponse(),
                sr.getAdminRespondedAt(),
                sr.getCreatedAt()
        );
    }

    static AdminServiceRequestResponse toAdminResponse(ServiceRequest sr) {
        return new AdminServiceRequestResponse(
                sr.getId(),
                sr.getRole(),
                sr.getUser().getName(),
                sr.getUser().getEmail(),
                sr.getType(),
                sr.getDescription(),
                sr.getStatus(),
                sr.getAdminResponse(),
                sr.getAdminNotes(),
                sr.getAdminRespondedAt(),
                sr.getCreatedAt()
        );
    }
}

package com.skipq.core.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    List<ServiceRequest> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}

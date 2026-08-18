package com.skipq.core.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID> {
    List<SubscriptionPayment> findAllByVendorIdOrderByPaidOnDesc(UUID vendorId);
    Optional<SubscriptionPayment> findFirstByVendorIdOrderByPaidOnDesc(UUID vendorId);
}

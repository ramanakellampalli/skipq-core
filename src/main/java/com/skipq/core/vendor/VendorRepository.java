package com.skipq.core.vendor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByIsOpenTrue();

    List<Vendor> findAllByOrderByIsOpenDesc();

    long countByIsOpenTrue();

    Optional<Vendor> findByUserId(UUID userId);

    Optional<Vendor> findByUserEmail(String email);

    Optional<Vendor> findByRazorpayLinkedAccountId(String razorpayLinkedAccountId);
}

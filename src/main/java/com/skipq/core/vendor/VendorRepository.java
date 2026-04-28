package com.skipq.core.vendor;

import com.skipq.core.campus.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByIsOpenTrue();

    List<Vendor> findAllByOrderByIsOpenDesc();

    List<Vendor> findAllByCampusOrderByIsOpenDesc(Campus campus);

    long countByIsOpenTrue();

    Optional<Vendor> findByUserId(UUID userId);

    Optional<Vendor> findByRazorpayLinkedAccountId(String razorpayLinkedAccountId);

    Optional<Vendor> findByUserEmail(String email);
}

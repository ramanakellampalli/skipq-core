package com.skipq.core.repository;

import com.skipq.core.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByIsOpenTrue();

    Optional<Vendor> findByUserId(UUID userId);
}

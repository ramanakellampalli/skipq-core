package com.skipq.core.campus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CampusRepository extends JpaRepository<Campus, UUID> {
    Optional<Campus> findByEmailDomain(String emailDomain);
}

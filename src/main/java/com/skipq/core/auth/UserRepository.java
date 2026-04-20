package com.skipq.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findBySetupToken(String setupToken);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.campus WHERE u.id = :id")
    Optional<User> findByIdWithCampus(@Param("id") UUID id);
}

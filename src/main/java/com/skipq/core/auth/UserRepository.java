package com.skipq.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("UPDATE User u SET u.avatarUrl = :avatarUrl WHERE u.id = :userId")
    void updateAvatarUrl(@Param("userId") UUID userId, @Param("avatarUrl") String avatarUrl);
}

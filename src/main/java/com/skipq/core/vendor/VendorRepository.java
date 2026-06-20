package com.skipq.core.vendor;

import com.skipq.core.campus.Campus;
import com.skipq.core.common.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByIsOpenTrueAndAccountStatus(AccountStatus accountStatus);

    List<Vendor> findAllByOrderByIsOpenDesc();

    List<Vendor> findAllByCampusAndAccountStatusOrderByIsOpenDesc(Campus campus, AccountStatus accountStatus);

    List<Vendor> findAllByCampusIsNullAndAccountStatusOrderByIsOpenDesc(AccountStatus accountStatus);

    long countByIsOpenTrue();

    Optional<Vendor> findByUserId(UUID userId);

    Optional<Vendor> findByRazorpayLinkedAccountId(String razorpayLinkedAccountId);

    Optional<Vendor> findByUserEmail(String email);

    @Modifying
    @Query("UPDATE Vendor v SET v.logoUrl = :logoUrl WHERE v.id = :vendorId")
    void updateLogoUrl(@Param("vendorId") UUID vendorId, @Param("logoUrl") String logoUrl);

    @Modifying
    @Query("UPDATE Vendor v SET v.logoUrl = :logoUrl WHERE v.user.id = :userId")
    void updateLogoUrlByUserId(@Param("userId") UUID userId, @Param("logoUrl") String logoUrl);
}

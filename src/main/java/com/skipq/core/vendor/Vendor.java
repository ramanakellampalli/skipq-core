package com.skipq.core.vendor;

import com.skipq.core.auth.User;
import com.skipq.core.campus.Campus;
import com.skipq.core.common.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "campus"})
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "campus_id", nullable = true)
    private Campus campus;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen;

    @Column(name = "prep_time", nullable = false)
    private int prepTime;

    // KYC fields
    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "bank_account", length = 30)
    private String bankAccount;

    @Column(name = "ifsc", length = 11)
    private String ifsc;

    @Column(name = "gst_registered", nullable = false)
    private boolean gstRegistered = false;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "kyc_approved", nullable = false)
    private boolean kycApproved = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "suspension_note", columnDefinition = "TEXT")
    private String suspensionNote;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "reset_otp", length = 6)
    private String resetOtp;

    @Column(name = "reset_otp_expires_at")
    private LocalDateTime resetOtpExpiresAt;

    @Column(name = "subscription_monthly_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subscriptionMonthlyPrice = BigDecimal.ZERO;

    @Column(name = "subscription_paid_through")
    private LocalDate subscriptionPaidThrough;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SubscriptionStatus computedSubscriptionStatus() {
        if (subscriptionMonthlyPrice.compareTo(BigDecimal.ZERO) > 0) {
            LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
            if (subscriptionPaidThrough == null || subscriptionPaidThrough.isBefore(firstOfMonth)) {
                return SubscriptionStatus.PAST_DUE;
            }
        }
        return SubscriptionStatus.ACTIVE;
    }
}

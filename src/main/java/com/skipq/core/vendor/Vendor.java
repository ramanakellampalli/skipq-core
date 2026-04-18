package com.skipq.core.vendor;

import com.skipq.core.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @Column(name = "razorpay_linked_acct_id", length = 50)
    private String razorpayLinkedAccountId;

    @Column(name = "kyc_approved", nullable = false)
    private boolean kycApproved = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

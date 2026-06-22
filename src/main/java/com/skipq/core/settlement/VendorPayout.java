package com.skipq.core.settlement;

import com.skipq.core.vendor.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "settlement_start_at", nullable = false)
    private LocalDateTime settlementStartAt;

    @Column(name = "settlement_cutoff_at", nullable = false)
    private LocalDateTime settlementCutoffAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "payout_reference", length = 255)
    private String payoutReference;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

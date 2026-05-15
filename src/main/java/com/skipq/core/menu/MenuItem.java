package com.skipq.core.menu;

import com.skipq.core.vendor.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"vendor", "variants"})
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "category", length = 100)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_veg", nullable = false)
    private boolean isVeg;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // kept for legacy reads — source of truth moves to menu_variants
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @OneToMany(mappedBy = "menuItem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private List<MenuVariant> variants = new ArrayList<>();

    public List<MenuVariant> getVariants() {
        return java.util.Collections.unmodifiableList(variants);
    }

    public void addVariant(MenuVariant variant) {
        variants.add(variant);
    }

    public void clearVariants() {
        variants.clear();
    }

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

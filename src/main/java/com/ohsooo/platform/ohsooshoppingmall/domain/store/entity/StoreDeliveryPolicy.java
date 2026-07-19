package com.ohsooo.platform.ohsooshoppingmall.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "store_delivery_policies")
public class StoreDeliveryPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_policy_id")
    private Long deliveryPolicyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "base_delivery_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseDeliveryFee;

    @Column(name = "free_delivery_threshold", precision = 19, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public StoreDeliveryPolicy(Store store, BigDecimal baseDeliveryFee, BigDecimal freeDeliveryThreshold) {
        this.store = store;
        this.baseDeliveryFee = baseDeliveryFee;
        this.freeDeliveryThreshold = freeDeliveryThreshold;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(BigDecimal baseDeliveryFee, BigDecimal freeDeliveryThreshold) {
        this.baseDeliveryFee = baseDeliveryFee;
        this.freeDeliveryThreshold = freeDeliveryThreshold;
    }
}

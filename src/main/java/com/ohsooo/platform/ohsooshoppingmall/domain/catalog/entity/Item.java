package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    // 평점: numeric(2,1) 이면 precision=2, scale=1 맞음
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Item(Store store, Category category, String name, BigDecimal basePrice) {
        this.store = store;
        this.category = category;
        this.name = name;

        // null 방어 + scale 통일(가격 정책에 맞춰 2자리 고정)
        this.basePrice = (basePrice == null)
            ? BigDecimal.ZERO.setScale(2)
            : basePrice.setScale(2);

        this.status = ItemStatus.ACTIVE;
        this.rating = BigDecimal.ZERO.setScale(1);
        this.reviewCount = 0;
        this.isDeleted = false;
    }

    // 기존 int로 넘기던 코드가 남아있으면 임시로 이 생성자도 유지해도 됨(점진 마이그레이션)
    public Item(Store store, Category category, String name, int basePrice) {
        this(store, category, name, BigDecimal.valueOf(basePrice));
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        // 혹시라도 null로 들어오는 케이스 방어
        if (this.basePrice == null) this.basePrice = BigDecimal.ZERO.setScale(2);
        if (this.rating == null) this.rating = BigDecimal.ZERO.setScale(1);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.status = ItemStatus.INACTIVE;
    }

    public void delete() {
        if (this.isDeleted) return;

        OffsetDateTime now = OffsetDateTime.now();
        this.isDeleted = true;
        this.deletedAt = now;
        this.status = ItemStatus.DELETED;
    }
}

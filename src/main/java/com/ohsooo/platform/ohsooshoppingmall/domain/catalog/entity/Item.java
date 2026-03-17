package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // 이거 있어도 되나?
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
    @Column(nullable = false)
    private ItemStatus status;

    @Column(name = "base_price", nullable = false)
    private int basePrice;

    // precision = 전체 자릿수(소수점 포함), scale = 소수점 이하 자릿수
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at",nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    //TODO: isdeleted와 status = DELETED는 같은 뜻
    @Column(name = "is_deleted",nullable = false)
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Item(Store store, Category category, String name, int basePrice){
        this.store = store;
        this.category = category;
        this.name = name;
        this.basePrice = basePrice;

        this.status = ItemStatus.ACTIVE;
        this.rating = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.isDeleted = false;
    }

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate(){this.status = ItemStatus.INACTIVE;}

    public void delete(){
        if (this.isDeleted) return;

        OffsetDateTime now = OffsetDateTime.now();
        this.isDeleted = true;
        this.deletedAt = now;
        this.status = ItemStatus.DELETED;
    }
}

package com.ohsooo.platform.ohsooshoppingmall.domain.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "store_banners")
public class StoreBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_banner_id")
    private Long storeBannerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "link_url", length = 2048)
    private String linkUrl;

    @Column(length = 255)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public StoreBanner(Store store, String imageUrl, String linkUrl, String title, int sortOrder) {
        this.store = store;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.title = title;
        this.sortOrder = sortOrder;
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

    public void update(String imageUrl, String linkUrl, String title, Boolean isActive) {
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (linkUrl != null) {
            this.linkUrl = linkUrl;
        }
        if (title != null) {
            this.title = title;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void deactivate() {
        this.isActive = false;
    }
}

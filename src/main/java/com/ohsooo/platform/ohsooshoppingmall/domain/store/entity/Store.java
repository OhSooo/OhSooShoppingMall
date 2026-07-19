package com.ohsooo.platform.ohsooshoppingmall.domain.store.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notice;

    @Column(name = "main_image_url", length = 500)
    private String mainImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_status", nullable = false)
    private StoreOperationStatus operationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Store(Long ownerId, String name, String description) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.status = StoreStatus.ACTIVE;
        this.operationStatus = StoreOperationStatus.OPEN;
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

    public void changeStatus(StoreStatus status) {
        this.status = status;
    }

    public void updateProfile(String description, String notice, String mainImageUrl) {
        if (description != null) {
            this.description = description;
        }
        if (notice != null) {
            this.notice = notice;
        }
        if (mainImageUrl != null) {
            this.mainImageUrl = mainImageUrl;
        }
    }

    public void changeOperationStatus(StoreOperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }
}
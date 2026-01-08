package com.ohsooo.platform.ohsooshoppingmall.store.dto;

import java.time.LocalDateTime;

import com.ohsooo.platform.ohsooshoppingmall.store.domain.Store;
import com.ohsooo.platform.ohsooshoppingmall.store.domain.StoreStatus;
import lombok.Getter;

@Getter
public class StoreResponse {

    private final Long storeId;
    private final Long ownerId;
    private final String name;
    private final String description;
    private final StoreStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private StoreResponse(
            Long storeId,
            Long ownerId,
            String name,
            String description,
            StoreStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.storeId = storeId;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getStoreId(),
                store.getOwnerId(),
                store.getName(),
                store.getDescription(),
                store.getStatus(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}

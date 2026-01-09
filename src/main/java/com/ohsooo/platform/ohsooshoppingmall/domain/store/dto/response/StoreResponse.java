package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response;

import java.time.LocalDateTime;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@Schema(description = "스토어 응답 DTO")
public class StoreResponse {

    @Schema(example = "1")
    private final Long storeId;

    @Schema(example = "123")
    private final Long ownerId;

    @Schema(example = "오수 전자상점")
    private final String name;

    @Schema(example = "전자기기 전문 스토어")
    private final String description;

    @Schema(example = "ACTIVE")
    private final StoreStatus status;

    @Schema(description = "생성 시각")
    private final LocalDateTime createdAt;

    @Schema(description = "마지막 수정 시각")
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

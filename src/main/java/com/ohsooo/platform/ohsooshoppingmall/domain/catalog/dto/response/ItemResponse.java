package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
public class ItemResponse {

    private final Long id;
    private final Long storeId;
    private final Long categoryId;
    private final String name;
    private final ItemStatus status;

    // int -> BigDecimal
    private final BigDecimal basePrice;

    private final BigDecimal rating;
    private final int reviewCount;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final boolean isDeleted;
    private final OffsetDateTime deletedAt;

    public ItemResponse(
        Long id,
        Long storeId,
        Long categoryId,
        String name,
        ItemStatus status,
        BigDecimal basePrice,
        BigDecimal rating,
        int reviewCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean isDeleted,
        OffsetDateTime deletedAt
    ) {
        this.id = id;
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.name = name;
        this.status = status;
        this.basePrice = basePrice;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
    }
}

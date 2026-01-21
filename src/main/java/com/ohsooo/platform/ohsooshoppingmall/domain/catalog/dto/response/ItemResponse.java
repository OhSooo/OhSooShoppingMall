package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Category;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
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
    private int basePrice;
    private BigDecimal rating;
    private int reviewCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private boolean isDeleted;
    private OffsetDateTime deletedAt;

    public ItemResponse(Long id, Long storeId, Long categoryId, String name, ItemStatus status, int basePrice, BigDecimal rating, int reviewCount, OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean isDeleted, OffsetDateTime deletedAt) {
        this.id = id;
        this.storeId= storeId;
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

package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class ItemImageResponse {

    private final Long imageId;
    private final Long itemId;
    private final String imageUrl;
    private final int displayOrder;

    @Getter(AccessLevel.NONE)
    private final boolean primary;

    private final String altText;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public ItemImageResponse(
        Long imageId,
        Long itemId,
        String imageUrl,
        int displayOrder,
        boolean isPrimary,
        String altText,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.imageId = imageId;
        this.itemId = itemId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.primary = isPrimary;
        this.altText = altText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @JsonProperty("isPrimary")
    public boolean isPrimary() {
        return primary;
    }
}

package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemDetailResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemImageResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemMapper {

    public ItemResponse toResponse(final Item item) {
        return new ItemResponse(
            item.getItemId(),
            item.getStore().getStoreId(),
            item.getCategory().getCategoryId(),
            item.getName(),
            item.getStatus(),
            item.getBasePrice(),
            item.getRating(),
            item.getReviewCount(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.isDeleted(),
            item.getDeletedAt()
        );
    }

    public ItemDetailResponse toDetailResponse(final Item item, final List<ItemImageResponse> images) {
        return new ItemDetailResponse(
            item.getItemId(),
            item.getStore().getStoreId(),
            item.getCategory().getCategoryId(),
            item.getName(),
            item.getStatus(),
            item.getBasePrice(),
            item.getRating(),
            item.getReviewCount(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.isDeleted(),
            item.getDeletedAt(),
            images
        );
    }
}

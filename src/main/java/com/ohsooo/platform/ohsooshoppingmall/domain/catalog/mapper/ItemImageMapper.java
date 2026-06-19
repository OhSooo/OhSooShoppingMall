package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemImageResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemImage;
import org.springframework.stereotype.Component;

@Component
public class ItemImageMapper {

    public ItemImageResponse toResponse(final ItemImage image) {
        return new ItemImageResponse(
            image.getItemImageId(),
            image.getItem().getItemId(),
            image.getImageUrl(),
            image.getDisplayOrder(),
            image.isPrimary(),
            image.getAltText(),
            image.getCreatedAt(),
            image.getUpdatedAt()
        );
    }
}

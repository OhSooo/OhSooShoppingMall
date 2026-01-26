package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantOptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariantOption;
import org.springframework.stereotype.Component;

@Component
public class ItemVariantOptionMapper {

    public ItemVariantOptionResponse toResponse(final ItemVariantOption itemVariantOption) {
        return new ItemVariantOptionResponse(
                itemVariantOption.getItemVariantOptionId(),
                itemVariantOption.getOption(),
                itemVariantOption.getItemVariant()
        );
    }
}

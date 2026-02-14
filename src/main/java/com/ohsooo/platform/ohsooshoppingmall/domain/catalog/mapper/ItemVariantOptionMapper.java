package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantOptionResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariantOption;
import org.springframework.stereotype.Component;

@Component
public class ItemVariantOptionMapper {

    public ItemVariantOptionResponse toResponse(final ItemVariantOption itemVariantOption) {
        return ItemVariantOptionResponse.builder()
                .id(itemVariantOption.getItemVariantOptionId())
                .optionId(itemVariantOption.getOption().getOptionId())
                .optionType(itemVariantOption.getOption().getType())
                .optionValue(itemVariantOption.getOption().getValue())
                .itemVariantId(itemVariantOption.getItemVariant().getItemVariantId())
                .build();
    }
}

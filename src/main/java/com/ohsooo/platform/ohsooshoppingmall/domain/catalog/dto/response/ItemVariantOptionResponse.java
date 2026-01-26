package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Option;
import lombok.Getter;

@Getter
public class ItemVariantOptionResponse {

    private final Long id;
    private final Option option;
    private final ItemVariant itemVariant;

    public ItemVariantOptionResponse(Long id, Option option, ItemVariant itemVariant) {
        this.id = id;
        this.option = option;
        this.itemVariant = itemVariant;
    }

}

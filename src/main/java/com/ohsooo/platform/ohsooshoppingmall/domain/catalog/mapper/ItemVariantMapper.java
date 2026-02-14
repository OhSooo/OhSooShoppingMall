package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariant;
import jakarta.persistence.Column;
import org.springframework.stereotype.Component;

@Component
public class ItemVariantMapper {

    public ItemVariantResponse toResponse(ItemVariant itemVariant) {

        Item item = itemVariant.getItem();

        return new ItemVariantResponse(
                itemVariant.getItemVariantId(),
                item.getItemId(),
                item.getName(),
                itemVariant.getSku(),
                itemVariant.getPrice(),
                itemVariant.getQuantity(),
                itemVariant.getStatus()
        );
    }
}

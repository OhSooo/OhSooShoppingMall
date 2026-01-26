package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariant;
import jakarta.persistence.Column;
import org.springframework.stereotype.Component;

@Component
public class ItemVariantMapper {

    public ItemVariantResponse toResponse(final ItemVariant itemVariant) {
        return new ItemVariantResponse(
                itemVariant.getItemVariantId(),
                itemVariant.getItem(),
                itemVariant.getSku(),
                itemVariant.getPrice(),
                itemVariant.getQuantity(),
                itemVariant.getStatus()
        );
    }
}

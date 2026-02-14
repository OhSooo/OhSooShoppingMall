package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class ItemVariantResponse {

    private final Long id;

    // Item 요약 정보
    private final Long itemId;
    private final String itemName;

    private final String sku;
    private final BigDecimal price;
    private final int quantity;
    private final ItemVariantStatus status;

    public ItemVariantResponse(
        Long id,
        Long itemId,
        String itemName,
        String sku,
        BigDecimal price,
        int quantity,
        ItemVariantStatus status
    ) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
    }
}

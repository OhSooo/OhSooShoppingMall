package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.Item;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemVariantStatus;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ItemVariantResponse {

    private final Long id;
    private final Item item;
    private final String sku;
    private final BigDecimal price;
    private final int quantity;
    private final ItemVariantStatus status;

    public ItemVariantResponse(Long id, Item item, String sku, BigDecimal price, int quantity, ItemVariantStatus status) {
        this.id = id;
        this.item = item;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
    }
}

package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.math.BigDecimal;
import java.util.List;

public class ItemCreateResponseDto {

    private final Long itemId;
    private final Long storeId;
    private final Long categoryId;
    private final String name;
    private final BigDecimal basePrice;
    private final ItemStatus status;
    private final List<VariantResult> variants;

    public ItemCreateResponseDto(
        Long itemId,
        Long storeId,
        Long categoryId,
        String name,
        BigDecimal basePrice,
        ItemStatus status,
        List<VariantResult> variants
    ) {
        this.itemId = itemId;
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.name = name;
        this.basePrice = basePrice;
        this.status = status;
        this.variants = variants;
    }

    public Long getItemId() { return itemId; }
    public Long getStoreId() { return storeId; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public BigDecimal getBasePrice() { return basePrice; }
    public ItemStatus getStatus() { return status; }
    public List<VariantResult> getVariants() { return variants; }

    public static class VariantResult {

        private final Long itemVariantId;
        private final String sku;
        private final BigDecimal price;
        private final ItemVariantStatus status;
        private final int initialQuantity;

        public VariantResult(
            Long itemVariantId,
            String sku,
            BigDecimal price,
            ItemVariantStatus status,
            int initialQuantity
        ) {
            this.itemVariantId = itemVariantId;
            this.sku = sku;
            this.price = price;
            this.status = status;
            this.initialQuantity = initialQuantity;
        }

        public Long getItemVariantId() { return itemVariantId; }
        public String getSku() { return sku; }
        public BigDecimal getPrice() { return price; }
        public ItemVariantStatus getStatus() { return status; }
        public int getInitialQuantity() { return initialQuantity; }
    }
}

package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.math.BigDecimal;
import java.util.List;

public class AddVariantsResponseDto {

    private final Long itemId;
    private final List<VariantResult> variants;

    public AddVariantsResponseDto(Long itemId, List<VariantResult> variants) {
        this.itemId = itemId;
        this.variants = variants;
    }

    public Long getItemId() { return itemId; }
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

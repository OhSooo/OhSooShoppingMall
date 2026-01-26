package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재고 상태")
public enum ItemVariantStatus {

    @Schema(description = "판매 중")
    AVAILABLE,

    @Schema(description = "품절")
    OUT_OF_STOCK,

    @Schema(description = "비활성화")
    DISABLED
}

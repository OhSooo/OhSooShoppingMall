package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 상태")
public enum ItemStatus {

    @Schema(description = "판매 중")
    ACTIVE,

    @Schema(description = "비활성화")
    INACTIVE,

    @Schema(description = "삭제됨 (soft delete)")
    DELETED
}

package com.ohsooo.platform.ohsooshoppingmall.domain.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스토어 영업 상태")
public enum StoreOperationStatus {

    @Schema(description = "영업 중")
    OPEN,

    @Schema(description = "영업 종료")
    CLOSED,

    @Schema(description = "일시 중지")
    PAUSED
}

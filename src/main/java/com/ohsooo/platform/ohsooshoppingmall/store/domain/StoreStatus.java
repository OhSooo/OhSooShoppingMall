package com.ohsooo.platform.ohsooshoppingmall.store.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스토어 상태")
public enum StoreStatus {

    @Schema(description = "운영 중")
    ACTIVE,

    @Schema(description = "비활성화")
    INACTIVE,

    @Schema(description = "관리자에 의해 정지")
    SUSPENDED,

    @Schema(description = "삭제됨 (soft delete)")
    DELETED
}

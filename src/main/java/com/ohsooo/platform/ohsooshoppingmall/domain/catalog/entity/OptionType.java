package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "옵션 타입")
public enum OptionType {
    // size, color
    @Schema(description = "크기")
    SIZE,

    @Schema(description = "색상")
    COLOR
}

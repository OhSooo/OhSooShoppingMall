package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Schema(description = "상품 기본 정보 수정 요청 DTO (Partial Update — null 필드는 기존 값 유지)")
public class ItemUpdateRequestDto {

    @Schema(description = "상품명", example = "반팔 티셔츠 (수정)")
    @Size(max = 255, message = "상품명은 255자 이내여야 합니다")
    private String name;

    @Schema(description = "카테고리 ID", example = "2")
    private Long categoryId;

    @Schema(description = "기본 가격", example = "25000")
    @DecimalMin(value = "1.0", inclusive = true, message = "basePrice는 1 이상이어야 합니다")
    private BigDecimal basePrice;

    protected ItemUpdateRequestDto() {}
}

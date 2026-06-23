package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "상품 이미지 수정 요청 DTO (Partial Update — null 필드는 기존 값 유지)")
public class ItemImageUpdateRequestDto {

    @Schema(description = "이미지 URL", example = "https://example.com/images/product1_v2.jpg")
    @Size(max = 2048, message = "이미지 URL은 2048자 이내여야 합니다")
    private String imageUrl;

    @Schema(description = "대표 이미지 여부", example = "true")
    private Boolean isPrimary;

    @Schema(description = "대체 텍스트", example = "상품 측면 사진")
    @Size(max = 255, message = "대체 텍스트는 255자 이내여야 합니다")
    private String altText;

    protected ItemImageUpdateRequestDto() {}
}

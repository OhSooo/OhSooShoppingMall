package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "상품 이미지 등록 요청 DTO")
public class ItemImageCreateRequestDto {

    @Schema(description = "이미지 URL", example = "https://example.com/images/product1.jpg", required = true)
    @NotBlank(message = "이미지 URL은 필수입니다")
    @Size(max = 2048, message = "이미지 URL은 2048자 이내여야 합니다")
    private String imageUrl;

    @Schema(description = "대표 이미지 여부 (기본값 false)", example = "false")
    private Boolean isPrimary;

    @Schema(description = "대체 텍스트", example = "상품 정면 사진")
    @Size(max = 255, message = "대체 텍스트는 255자 이내여야 합니다")
    private String altText;

    protected ItemImageCreateRequestDto() {}
}

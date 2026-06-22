package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 배너 수정 요청 DTO")
public class StoreBannerUpdateRequest {

    @Size(max = 2048, message = "배너 이미지 URL은 2048자 이내여야 합니다")
    @Schema(description = "배너 이미지 URL", example = "https://example.com/banner.jpg")
    private String imageUrl;

    @Size(max = 2048, message = "링크 URL은 2048자 이내여야 합니다")
    @Schema(description = "배너 클릭 시 이동 URL", example = "https://example.com/event")
    private String linkUrl;

    @Size(max = 255, message = "배너 제목은 255자 이내여야 합니다")
    @Schema(description = "배너 제목", example = "여름 세일 이벤트")
    private String title;

    @Schema(description = "활성 여부", example = "true")
    private Boolean isActive;
}

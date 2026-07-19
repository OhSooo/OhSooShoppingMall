package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 프로필 수정 요청 DTO")
public class StoreProfileUpdateRequest {

    @Schema(description = "스토어 소개", example = "최고의 전자기기를 판매합니다")
    private String description;

    @Schema(description = "스토어 공지", example = "6월 이벤트 진행 중!")
    private String notice;

    @Schema(description = "스토어 대표 이미지 URL", example = "https://example.com/image.jpg")
    private String mainImageUrl;
}

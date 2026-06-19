package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 배너 수정 요청 DTO")
public class StoreBannerUpdateRequest {

    @Schema(description = "배너 이미지 URL", example = "https://example.com/banner.jpg")
    private String imageUrl;

    @Schema(description = "배너 클릭 시 이동 URL", example = "https://example.com/event")
    private String linkUrl;

    @Schema(description = "배너 제목", example = "여름 세일 이벤트")
    private String title;

    @Schema(description = "정렬 순서", example = "1")
    private Integer sortOrder;

    @Schema(description = "활성 여부", example = "true")
    private Boolean isActive;
}

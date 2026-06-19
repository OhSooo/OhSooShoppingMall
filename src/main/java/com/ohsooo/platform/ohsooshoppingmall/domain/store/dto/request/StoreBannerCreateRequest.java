package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 배너 등록 요청 DTO")
public class StoreBannerCreateRequest {

    @NotBlank
    @Schema(description = "배너 이미지 URL", example = "https://example.com/banner.jpg")
    private String imageUrl;

    @Schema(description = "배너 클릭 시 이동 URL", example = "https://example.com/event")
    private String linkUrl;

    @Schema(description = "배너 제목", example = "여름 세일 이벤트")
    private String title;

    @Schema(description = "정렬 순서", example = "0")
    private Integer sortOrder;
}

package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Builder
@Getter
@Schema(description = "스토어 배너 응답 DTO")
public class StoreBannerResponse {

    @Schema(example = "1")
    private final Long storeBannerId;

    @Schema(example = "1")
    private final Long storeId;

    @Schema(example = "https://example.com/banner.jpg")
    private final String imageUrl;

    @Schema(example = "https://example.com/event")
    private final String linkUrl;

    @Schema(example = "여름 세일 이벤트")
    private final String title;

    @Schema(example = "0")
    private final int sortOrder;

    @Schema(description = "생성 시각")
    private final OffsetDateTime createdAt;

    @Schema(description = "마지막 수정 시각")
    private final OffsetDateTime updatedAt;
}

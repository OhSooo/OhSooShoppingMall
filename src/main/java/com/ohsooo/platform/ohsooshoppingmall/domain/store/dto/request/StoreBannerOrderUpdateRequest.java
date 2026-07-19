package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 배너 순서 변경 요청 DTO")
public class StoreBannerOrderUpdateRequest {

    @NotEmpty(message = "배너 ID 목록은 비어 있을 수 없습니다")
    @Schema(description = "정렬 순서대로 나열한 배너 ID 목록", example = "[3, 1, 2]")
    private List<Long> bannerIds;
}

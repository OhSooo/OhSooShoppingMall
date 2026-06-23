package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "상품 이미지 순서 변경 요청 DTO")
public class ItemImageOrderUpdateRequestDto {

    @Schema(description = "순서대로 정렬된 이미지 ID 목록", example = "[3, 1, 4, 2]", required = true)
    @NotEmpty(message = "이미지 ID 목록은 비어 있을 수 없습니다")
    private List<Long> imageIds;

    protected ItemImageOrderUpdateRequestDto() {}
}

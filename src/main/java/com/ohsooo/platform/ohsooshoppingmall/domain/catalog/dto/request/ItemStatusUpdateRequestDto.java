package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "상품 상태 변경 요청 DTO")
public class ItemStatusUpdateRequestDto {

    @Schema(description = "변경할 상품 상태 (ACTIVE, INACTIVE만 허용)", example = "INACTIVE")
    @NotNull(message = "status는 필수입니다")
    private ItemStatus status;

    protected ItemStatusUpdateRequestDto() {}
}

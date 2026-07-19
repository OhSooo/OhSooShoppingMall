package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreOperationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 영업 상태 변경 요청 DTO")
public class StoreOperationStatusUpdateRequest {

    @NotNull(message = "operationStatus는 필수입니다")
    @Schema(description = "변경할 영업 상태", example = "OPEN")
    private StoreOperationStatus operationStatus;
}

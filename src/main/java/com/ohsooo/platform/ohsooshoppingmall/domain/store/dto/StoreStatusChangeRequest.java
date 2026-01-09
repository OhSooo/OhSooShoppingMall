package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.domain.StoreStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 상태 변경 요청 DTO")
public class StoreStatusChangeRequest {

    @Schema(
            description = "변경할 스토어 상태",
            example = "INACTIVE"
    )
    private StoreStatus status;
}

package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "스토어 배송정책 등록/수정 요청 DTO")
public class StoreDeliveryPolicyUpsertRequest {

    @NotNull
    @Schema(description = "기본 배송비", example = "3000.00")
    private BigDecimal baseDeliveryFee;

    @Schema(description = "무료배송 기준 금액", example = "50000.00")
    private BigDecimal freeDeliveryThreshold;
}

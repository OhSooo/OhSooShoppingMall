package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Getter
@Schema(description = "스토어 배송정책 응답 DTO")
public class StoreDeliveryPolicyResponse {

    @Schema(example = "1")
    private final Long deliveryPolicyId;

    @Schema(example = "1")
    private final Long storeId;

    @Schema(example = "3000.00")
    private final BigDecimal baseDeliveryFee;

    @Schema(example = "50000.00")
    private final BigDecimal freeDeliveryThreshold;

    @Schema(example = "true")
    private final boolean isActive;

    @Schema(description = "생성 시각")
    private final OffsetDateTime createdAt;

    @Schema(description = "마지막 수정 시각")
    private final OffsetDateTime updatedAt;
}

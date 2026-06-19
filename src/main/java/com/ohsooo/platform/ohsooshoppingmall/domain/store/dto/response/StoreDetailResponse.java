package com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreOperationStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.StoreStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Getter
@Schema(description = "스토어 상세 응답 DTO")
public class StoreDetailResponse {

    @Schema(example = "1")
    private final Long storeId;

    @Schema(example = "123")
    private final Long ownerId;

    @Schema(example = "오수 전자상점")
    private final String name;

    @Schema(example = "전자기기 전문 스토어")
    private final String description;

    @Schema(example = "6월 이벤트 진행 중!")
    private final String notice;

    @Schema(example = "https://example.com/image.jpg")
    private final String mainImageUrl;

    @Schema(example = "ACTIVE")
    private final StoreStatus status;

    @Schema(example = "OPEN")
    private final StoreOperationStatus operationStatus;

    @Schema(description = "활성 배너 목록")
    private final List<StoreBannerResponse> banners;

    @Schema(description = "배송정책 (없을 수 있음)")
    private final StoreDeliveryPolicyResponse deliveryPolicy;

    @Schema(description = "생성 시각")
    private final OffsetDateTime createdAt;

    @Schema(description = "마지막 수정 시각")
    private final OffsetDateTime updatedAt;
}

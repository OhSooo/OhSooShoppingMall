package com.ohsooo.platform.ohsooshoppingmall.domain.store.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreDeliveryPolicyUpsertRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreDeliveryPolicyResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.service.StoreDeliveryPolicyService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores/{storeId}/delivery-policy")
@Tag(name = "Store Delivery Policy", description = "스토어 배송정책 관리 API")
@RequiredArgsConstructor
public class StoreDeliveryPolicyController {

    private final StoreDeliveryPolicyService storeDeliveryPolicyService;

    @Operation(summary = "배송정책 조회", description = "스토어의 배송정책을 조회합니다")
    @GetMapping
    public ResponseEntity<BaseResponse<StoreDeliveryPolicyResponse>> getDeliveryPolicy(
            @PathVariable Long storeId
    ) {
        StoreDeliveryPolicyResponse response = storeDeliveryPolicyService.getDeliveryPolicy(storeId);
        return ResponseEntity.ok(BaseResponse.success("배송정책 조회 성공", response));
    }

    @Operation(summary = "배송정책 등록/수정", description = "스토어 owner가 배송정책을 등록하거나 수정합니다")
    @PutMapping
    public ResponseEntity<BaseResponse<StoreDeliveryPolicyResponse>> upsertDeliveryPolicy(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreDeliveryPolicyUpsertRequest request
    ) {
        StoreDeliveryPolicyResponse response = storeDeliveryPolicyService.upsertDeliveryPolicy(
                storeId, userId,
                request.getBaseDeliveryFee(), request.getFreeDeliveryThreshold());
        return ResponseEntity.ok(BaseResponse.success("배송정책 저장 성공", response));
    }
}

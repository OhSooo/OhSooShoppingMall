package com.ohsooo.platform.ohsooshoppingmall.domain.store.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.CreateStoreRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreOperationStatusUpdateRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreProfileUpdateRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreDetailResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreProfileResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.service.StoreQueryService;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.service.StoreService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
@Tag(name = "Store", description = "스토어 관리 API")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final StoreQueryService storeQueryService;

    @Operation(summary = "스토어 생성", description = "관리자가 새로운 스토어를 생성합니다")
    @PostMapping
    public ResponseEntity<BaseResponse<StoreResponse>> createStore(
            @AuthenticationPrincipal Long userId,
            @RequestBody CreateStoreRequest request
    ) {
        StoreResponse response = storeService.createStore(
                userId, request.getName(), request.getDescription());
        return ResponseEntity.ok(BaseResponse.success("스토어 생성 성공", response));
    }

    @Operation(summary = "스토어 목록 조회", description = "활성화된(ACTIVE) 스토어 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<BaseResponse<List<StoreResponse>>> getStores() {
        List<StoreResponse> response = storeService.getActiveStores();
        return ResponseEntity.ok(BaseResponse.success("전체 스토어 조회 성공", response));
    }

    @Operation(summary = "스토어 상세 조회", description = "스토어 ID로 스토어 상세 정보를 조회합니다 (배너, 배송정책 포함)")
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreDetailResponse>> getStore(
            @PathVariable Long storeId
    ) {
        StoreDetailResponse response = storeQueryService.getStoreDetail(storeId);
        return ResponseEntity.ok(BaseResponse.success("스토어 조회 성공", response));
    }

    @Operation(summary = "스토어 프로필 수정", description = "스토어 owner가 프로필(소개, 공지, 대표이미지)을 수정합니다")
    @PatchMapping("/{storeId}/profile")
    public ResponseEntity<BaseResponse<StoreProfileResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @RequestBody StoreProfileUpdateRequest request
    ) {
        StoreProfileResponse response = storeService.updateProfile(
                storeId, userId,
                request.getDescription(), request.getNotice(), request.getMainImageUrl());
        return ResponseEntity.ok(BaseResponse.success("스토어 프로필 수정 성공", response));
    }

    @Operation(summary = "스토어 영업 상태 변경", description = "스토어 owner가 영업 상태(OPEN/CLOSED/PAUSED)를 변경합니다")
    @PatchMapping("/{storeId}/operation-status")
    public ResponseEntity<BaseResponse<StoreProfileResponse>> changeOperationStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @RequestBody StoreOperationStatusUpdateRequest request
    ) {
        StoreProfileResponse response = storeService.changeOperationStatus(
                storeId, userId, request.getOperationStatus());
        return ResponseEntity.ok(BaseResponse.success("스토어 영업 상태 변경 성공", response));
    }

}

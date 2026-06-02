package com.ohsooo.platform.ohsooshoppingmall.domain.store.controller;


import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreStatusChangeRequest;
import com.ohsooo.platform.ohsooshoppingmall.global.response.ApiResponse;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.entity.Store;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.CreateStoreRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.service.StoreService;

import java.util.List;

@RestController
@RequestMapping("/stores")
@Tag(name = "Store", description = "스토어 관리 API")
public class StoreController {

    private final StoreService storeService;
    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(
            summary = "스토어 생성",
            description = "관리자가 새로운 스토어를 생성합니다"
    )
    @PostMapping
    public ResponseEntity<BaseResponse<StoreResponse>> createStore(
            @AuthenticationPrincipal Long userId,
            @RequestBody CreateStoreRequest request
    ) {
        StoreResponse response = storeService.createStore(
                userId,
                request.getName(),
                request.getDescription()
        );

        return ResponseEntity.ok(
                BaseResponse.success("스토어 생성 성공", response)
        );
    }

    @Operation(
            summary = "스토어 목록 조회",
            description = "활성화된(ACTIVE) 스토어 목록을 조회합니다"
    )
    @GetMapping
    public ResponseEntity<BaseResponse<List<StoreResponse>>> getStores() {


        List<StoreResponse> response = storeService.getActiveStores();
        return ResponseEntity.ok(BaseResponse.success("전체 스토어 조회 성공", response));


    }

    @Operation(
            summary = "스토어 단건 조회",
            description = "스토어 ID로 스토어 상세 정보를 조회합니다"
    )
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreResponse>> getStore(
            @PathVariable Long storeId
    ) {
        return ResponseEntity.ok(
                BaseResponse.success("스토어 조회 성공", storeService.getStore(storeId))
        );
    }

    @Operation(
            summary = "스토어 상태 변경 (Owner)",
            description = "스토어 owner가 자신의 스토어 상태를 변경합니다"
    )
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<Void> changeStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @RequestBody StoreStatusChangeRequest request
    ) {
        storeService.changeStatusByOwner(storeId, userId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}

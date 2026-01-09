package com.ohsooo.platform.ohsooshoppingmall.domain.store.controller;


import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreStatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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

    // TODO: ROLE_ADMIN 사용자만 스토어 생성 가능하도록 권한 체크 추가
    @Operation(
            summary = "스토어 생성",
            description = "관리자가 새로운 스토어를 생성합니다"
    )
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @RequestBody CreateStoreRequest request
    ) {
        Store store = storeService.createStore(
                request.getOwnerId(),
                request.getName(),
                request.getDescription()
        );

        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @Operation(
            summary = "스토어 목록 조회",
            description = "활성화된(ACTIVE) 스토어 목록을 조회합니다"
    )
    @GetMapping
    public List<StoreResponse> getStores() {
        return storeService.getActiveStores()
                .stream()
                .map(StoreResponse::from)
                .toList();
    }

    @Operation(
            summary = "스토어 단건 조회",
            description = "스토어 ID로 스토어 상세 정보를 조회합니다"
    )
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(
            @PathVariable Long storeId
    ) {
        Store store = storeService.getStore(storeId);
        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @Operation(
            summary = "스토어 상태 변경 (Owner)",
            description = "스토어 owner가 자신의 스토어 상태를 변경합니다"
    )
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long storeId,
            @RequestBody StoreStatusChangeRequest request
    ) {
        Long ownerId = 1L; // TODO: JWT에서 추출
        storeService.changeStatusByOwner(storeId, ownerId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}

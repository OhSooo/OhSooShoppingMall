package com.ohsooo.platform.ohsooshoppingmall.domain.store.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreStatusChangeRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/stores")
@Tag(name = "Admin Store", description = "관리자용 스토어 관리 API")
public class AdminStoreController {
    private final StoreService storeService;

    public AdminStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(
            summary = "스토어 상태 변경 (관리자)",
            description = "관리자가 스토어 상태를 강제로 변경합니다"
    )
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<Void> changeStatusByAdmin(
            @PathVariable Long storeId,
            @RequestBody StoreStatusChangeRequest request
    ) {
        storeService.changeStatusByAdmin(storeId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}

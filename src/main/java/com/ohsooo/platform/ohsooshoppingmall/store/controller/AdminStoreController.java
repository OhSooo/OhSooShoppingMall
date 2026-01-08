package com.ohsooo.platform.ohsooshoppingmall.store.controller;

import com.ohsooo.platform.ohsooshoppingmall.store.dto.StoreStatusChangeRequest;
import com.ohsooo.platform.ohsooshoppingmall.store.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/stores")
public class AdminStoreController {
    private final StoreService storeService;

    public AdminStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PatchMapping("/{storeId}/status")
    public ResponseEntity<Void> changeStatusByAdmin(
            @PathVariable Long storeId,
            @RequestBody StoreStatusChangeRequest request
    ) {
        storeService.changeStatusByAdmin(storeId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}

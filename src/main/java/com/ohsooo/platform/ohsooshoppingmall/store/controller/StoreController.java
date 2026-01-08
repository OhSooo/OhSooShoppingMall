package com.ohsooo.platform.ohsooshoppingmall.store.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ohsooo.platform.ohsooshoppingmall.store.domain.Store;
import com.ohsooo.platform.ohsooshoppingmall.store.dto.CreateStoreRequest;
import com.ohsooo.platform.ohsooshoppingmall.store.dto.StoreResponse;
import com.ohsooo.platform.ohsooshoppingmall.store.service.StoreService;

@RestController
@RequestMapping("/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // TODO: ROLE_ADMIN 사용자만 스토어 생성 가능하도록 권한 체크 추가
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

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStore(
            @PathVariable Long storeId
    ) {
        Store store = storeService.getStore(storeId);
        return ResponseEntity.ok(StoreResponse.from(store));
    }
}

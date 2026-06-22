package com.ohsooo.platform.ohsooshoppingmall.domain.store.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreBannerCreateRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreBannerOrderUpdateRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.request.StoreBannerUpdateRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.dto.response.StoreBannerResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.store.service.StoreBannerService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores/{storeId}/banners")
@Tag(name = "Store Banner", description = "스토어 배너 관리 API")
@RequiredArgsConstructor
public class StoreBannerController {

    private final StoreBannerService storeBannerService;

    @Operation(summary = "배너 등록", description = "스토어 owner가 배너를 등록합니다. 순서는 마지막에 자동 추가됩니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<StoreBannerResponse>> createBanner(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreBannerCreateRequest request
    ) {
        StoreBannerResponse response = storeBannerService.createBanner(
                storeId, userId,
                request.getImageUrl(), request.getLinkUrl(), request.getTitle());
        return ResponseEntity.ok(BaseResponse.success("배너 등록 성공", response));
    }

    @Operation(summary = "배너 수정", description = "스토어 owner가 배너 내용을 수정합니다. 순서 변경은 별도 API를 사용하세요.")
    @PatchMapping("/{bannerId}")
    public ResponseEntity<BaseResponse<StoreBannerResponse>> updateBanner(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @PathVariable Long bannerId,
            @Valid @RequestBody StoreBannerUpdateRequest request
    ) {
        StoreBannerResponse response = storeBannerService.updateBanner(
                storeId, bannerId, userId,
                request.getImageUrl(), request.getLinkUrl(),
                request.getTitle(), request.getIsActive());
        return ResponseEntity.ok(BaseResponse.success("배너 수정 성공", response));
    }

    @Operation(summary = "배너 순서 변경", description = "스토어 owner가 활성 배너의 노출 순서를 변경합니다")
    @PatchMapping("/order")
    public ResponseEntity<BaseResponse<List<StoreBannerResponse>>> reorderBanners(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @Valid @RequestBody StoreBannerOrderUpdateRequest request
    ) {
        List<StoreBannerResponse> response = storeBannerService.reorderBanners(
                storeId, userId, request.getBannerIds());
        return ResponseEntity.ok(BaseResponse.success("배너 순서 변경 성공", response));
    }

    @Operation(summary = "배너 삭제 (비활성화)", description = "스토어 owner가 배너를 비활성화합니다")
    @DeleteMapping("/{bannerId}")
    public ResponseEntity<Void> deactivateBanner(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @PathVariable Long bannerId
    ) {
        storeBannerService.deactivateBanner(storeId, bannerId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "배너 목록 조회", description = "활성화된 배너를 정렬 순서대로 조회합니다")
    @GetMapping
    public ResponseEntity<BaseResponse<List<StoreBannerResponse>>> getBanners(
            @PathVariable Long storeId
    ) {
        List<StoreBannerResponse> response = storeBannerService.getActiveBanners(storeId);
        return ResponseEntity.ok(BaseResponse.success("배너 목록 조회 성공", response));
    }
}

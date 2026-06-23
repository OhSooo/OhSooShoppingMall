package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemImageCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemImageOrderUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemImageUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemImageResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.ItemImageService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/item/{itemId}/images")
@RequiredArgsConstructor
@Tag(name = "ItemImage", description = "상품 이미지 API")
public class ItemImageController {

    private final ItemImageService itemImageService;

    @Operation(
            summary = "상품 이미지 등록",
            description = """
        상품에 이미지 메타데이터를 등록합니다.

        - 이미지 URL은 필수값입니다.
        - 상품당 최대 10개까지 등록 가능합니다.
        - displayOrder는 서버에서 자동 부여됩니다 (현재 최대값 + 1).
        - isPrimary=true로 등록하면 기존 대표 이미지는 자동으로 해제됩니다.
        - 스토어 소유자만 등록할 수 있습니다.
        """
    )
    @PostMapping
    public ResponseEntity<BaseResponse<ItemImageResponse>> addImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemImageCreateRequestDto request
    ) {
        ItemImageResponse response = itemImageService.addImage(userId, itemId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("이미지 등록 성공", response));
    }

    @Operation(
            summary = "상품 이미지 목록 조회",
            description = """
        상품에 등록된 이미지 목록을 조회합니다.

        - displayOrder 오름차순으로 정렬됩니다.
        - 삭제된 상품은 조회되지 않습니다.
        """
    )
    @GetMapping
    public ResponseEntity<BaseResponse<List<ItemImageResponse>>> getImages(
            @PathVariable Long itemId
    ) {
        List<ItemImageResponse> response = itemImageService.getImagesByItemId(itemId);
        return ResponseEntity.ok(BaseResponse.success("이미지 조회 성공", response));
    }

    @Operation(
            summary = "상품 이미지 수정",
            description = """
        상품 이미지의 메타데이터(URL, 대표 이미지 여부, 대체 텍스트)를 수정합니다.
        Partial Update — null 필드는 기존 값을 유지합니다.
        순서 변경은 별도 순서 변경 API를 사용해주세요.

        - 스토어 소유자만 수정할 수 있습니다.
        - isPrimary=true로 변경하면 기존 대표 이미지는 자동으로 해제됩니다.
        """
    )
    @PatchMapping("/{imageId}")
    public ResponseEntity<BaseResponse<ItemImageResponse>> updateImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @RequestBody @Valid ItemImageUpdateRequestDto request
    ) {
        ItemImageResponse response = itemImageService.updateImage(userId, itemId, imageId, request);
        return ResponseEntity.ok(BaseResponse.success("이미지 수정 성공", response));
    }

    @Operation(
            summary = "상품 이미지 순서 변경",
            description = """
        상품 이미지의 노출 순서를 변경합니다.
        프론트에서 드래그 앤 드롭으로 정렬한 최종 이미지 ID 배열을 전송합니다.

        - imageIds 배열의 순서대로 displayOrder를 1부터 재부여합니다.
        - 해당 상품의 전체 이미지 ID를 모두 포함해야 합니다.
        - 스토어 소유자만 변경할 수 있습니다.
        """
    )
    @PatchMapping("/order")
    public ResponseEntity<BaseResponse<List<ItemImageResponse>>> updateImageOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemImageOrderUpdateRequestDto request
    ) {
        List<ItemImageResponse> response = itemImageService.updateImageOrder(userId, itemId, request);
        return ResponseEntity.ok(BaseResponse.success("이미지 순서 변경 성공", response));
    }

    @Operation(
            summary = "상품 이미지 삭제",
            description = """
        상품에 등록된 이미지를 삭제합니다.

        - 스토어 소유자만 삭제할 수 있습니다.
        - 해당 상품에 속한 이미지만 삭제할 수 있습니다.
        """
    )
    @DeleteMapping("/{imageId}")
    public ResponseEntity<BaseResponse<Void>> deleteImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @PathVariable Long imageId
    ) {
        itemImageService.deleteImage(userId, itemId, imageId);
        return ResponseEntity.ok(BaseResponse.success("이미지 삭제 성공", null));
    }
}

package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.AddVariantsRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemStatusUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemUpdateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.AddVariantsResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemDetailResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.ItemService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalog/item")
@RequiredArgsConstructor
@Tag(name="Item", description="상품 조회 API")
public class ItemController {

    private final ItemService itemService;

    @Operation(
            summary = "상품 생성",
            description = """
        스토어에 새 상품을 등록합니다. Item, Option, ItemVariant, ItemVariantOption,
        Inventory 초기 재고가 한 번에 생성됩니다.

        - SKU는 전체 시스템에서 고유해야 합니다.
        - 같은 요청 내 variants에서 SKU 중복은 허용되지 않습니다.
        - 동일 상품 내에서 type+value가 같은 옵션은 내부적으로 한 번만 생성됩니다.
        - initialQuantity는 1 이상이어야 합니다.
        """
    )
    @PostMapping
    public ResponseEntity<BaseResponse<ItemCreateResponseDto>> createItem(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ItemCreateRequestDto request
    ) {
        ItemCreateResponseDto response = itemService.createItem(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("상품 생성 성공", response));
    }

    @Operation(
            summary = "기존 상품에 Variant 추가",
            description = """
                기존 상품에 새로운 판매 단위(Variant)를 추가합니다.
                Option, ItemVariant, ItemVariantOption, Inventory가 한 트랜잭션으로 생성됩니다.

                - SKU는 전체 시스템에서 고유해야 합니다.
                - 같은 요청 내 variants에서 SKU 중복은 허용되지 않습니다.
                - 동일 상품 내에서 type+value가 같은 옵션은 기존 옵션을 재사용합니다.
                - initialQuantity는 0 이상이어야 합니다.
                - 판매자 권한이 필요합니다.
                """
    )
    @PostMapping("/{itemId}/variants")
    public ResponseEntity<BaseResponse<AddVariantsResponseDto>> addVariants(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @RequestBody @Valid AddVariantsRequestDto request
    ) {
        AddVariantsResponseDto response = itemService.addVariants(userId, itemId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("Variant 추가 성공", response));
    }

    @Operation(
            summary = "상품 단건 조회",
            description = """
        상품 ID를 기준으로 상품을 단건 조회합니다.
        
        - 삭제된 상품(isDeleted=true)은 조회되지 않습니다.
        - 존재하지 않는 상품 ID인 경우 오류를 반환합니다.
        """
    )
    @GetMapping("/{itemId}")
    public ResponseEntity<BaseResponse<ItemDetailResponse>> getItem(
            @PathVariable Long itemId
    ) {
        ItemDetailResponse response = itemService.getItemById(itemId);
        return ResponseEntity.ok(BaseResponse.success("상품 조회 성공", response));
    }

    @Operation(
            summary = "카테고리 기준 상품 목록 조회",
            description = """
        특정 카테고리에 속한 상품 목록을 조회합니다.
        
        - 하위 카테고리는 포함하지 않습니다.
        - 삭제된 상품은 제외됩니다.
        - 상품이 없는 경우 빈 배열을 반환합니다.
        """
    )
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<BaseResponse<List<ItemResponse>>> getItemsByCategory(
            @PathVariable Long categoryId
    ){
        List<ItemResponse> response = itemService.getItemsByCategory(categoryId);
        return ResponseEntity.ok(BaseResponse.success("상품 조회 성공", response));
    }


    @Operation(
            summary = "스토어 기준 상품 목록 조회",
            description = """
        특정 스토어에서 판매 중인 상품 목록을 조회합니다.
        
        - 삭제된 상품은 제외됩니다.
        - 상품이 없는 경우 빈 배열을 반환합니다.
        """
    )

    @GetMapping("/store/{storeId}")
    public ResponseEntity<BaseResponse<List<ItemResponse>>> getItemsByStore(
            @PathVariable Long storeId
    ){
        List<ItemResponse> response = itemService.getItemsByStore(storeId);
        return ResponseEntity.ok(BaseResponse.success("상품 조회 성공", response));
    }

    @Operation(
            summary = "전체 상품 목록 조회 (페이징 / 정렬)",
            description = """
        전체 상품 목록을 페이징 및 정렬 조건과 함께 조회합니다.
        
        기본 규칙:
        - page: 0부터 시작
        - size: 페이지당 상품 개수
        - sort: 정렬 기준 (예: createdAt,desc)
        
        예시:
        /catalog/item?page=0&size=20&sort=createdAt,desc
        
        - 삭제된 상품은 조회되지 않습니다.
        """
    )
    @GetMapping
    public ResponseEntity<BaseResponse<Page<ItemResponse>>> getItems(
            @ParameterObject Pageable pageable
    ) {
        Page<ItemResponse> response = itemService.getItems(pageable);
        return ResponseEntity.ok(BaseResponse.success("상품 조회 성공", response));
    }

    @Operation(
            summary = "상품 기본 정보 수정",
            description = """
        상품의 기본 정보(이름, 카테고리, 기본가격)를 수정합니다.
        Partial Update — null 필드는 기존 값을 유지합니다.

        - 스토어 소유자만 수정할 수 있습니다.
        - 삭제된 상품은 수정할 수 없습니다.
        - categoryId를 변경할 경우 활성 상태인 카테고리만 허용됩니다.
        """
    )
    @PatchMapping("/{itemId}")
    public ResponseEntity<BaseResponse<ItemResponse>> updateItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemUpdateRequestDto request
    ) {
        ItemResponse response = itemService.updateItem(userId, itemId, request);
        return ResponseEntity.ok(BaseResponse.success("상품 수정 성공", response));
    }

    @Operation(
            summary = "상품 상태 변경",
            description = """
        상품의 판매 상태를 변경합니다.

        - 스토어 소유자만 변경할 수 있습니다.
        - ACTIVE, INACTIVE만 허용됩니다.
        - DELETED 상태로의 직접 변경은 허용되지 않습니다 (삭제 API를 사용하세요).
        """
    )
    @PatchMapping("/{itemId}/status")
    public ResponseEntity<BaseResponse<ItemResponse>> updateItemStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemStatusUpdateRequestDto request
    ) {
        ItemResponse response = itemService.updateItemStatus(userId, itemId, request);
        return ResponseEntity.ok(BaseResponse.success("상품 상태 변경 성공", response));
    }

    @Operation(
            summary = "상품 삭제 (Soft Delete)",
            description = """
        상품을 소프트 삭제합니다.

        - 스토어 소유자만 삭제할 수 있습니다.
        - isDeleted=true, status=DELETED로 변경되며, deletedAt이 기록됩니다.
        - 이미 삭제된 상품은 조회되지 않습니다.
        """
    )
    @DeleteMapping("/{itemId}")
    public ResponseEntity<BaseResponse<Void>> deleteItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId
    ) {
        itemService.deleteItem(userId, itemId);
        return ResponseEntity.ok(BaseResponse.success("상품 삭제 성공", null));
    }
}

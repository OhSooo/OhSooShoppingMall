package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.request.ItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemCreateResponseDto;
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
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestBody @Valid ItemCreateRequestDto request
    ) {
        ItemCreateResponseDto response = itemService.createItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("상품 생성 성공", response));
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
    public ResponseEntity<BaseResponse<ItemResponse>> getItem(
            @PathVariable Long itemId
    ) {
        ItemResponse response = itemService.getItemById(itemId);
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
}

package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.ItemVariantService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/itemVariant")
@RequiredArgsConstructor
@Tag(name = "item variant", description="상품 단위 조회 API")
public class ItemVariantController {

    private final ItemVariantService itemVariantService;

    @Operation(
            summary = "상품 단위 단건 조회 (ID)",
            description = """
                상품 판매 단위(ItemVariant)를 ID 기준으로 조회합니다.
                
                - itemVariantId를 통해 단일 상품 단위를 조회합니다.
                - 존재하지 않는 ID일 경우 오류를 반환합니다.
                """
    )
    @GetMapping("/{itemVariantId}")
    public ResponseEntity<BaseResponse<ItemVariantResponse>> getItemVariantById(
            @PathVariable Long itemVariantId
    ) {
        ItemVariantResponse response = itemVariantService.findById(itemVariantId);
        return ResponseEntity.ok(BaseResponse.success("상품 단위 조회 성공", response));
    }

    @Operation(
            summary = "상품 단위 단건 조회 (SKU)",
            description = """
                상품 판매 단위(ItemVariant)를 SKU 기준으로 조회합니다.
                
                - SKU는 상품 옵션 조합을 식별하는 고유 값입니다.
                - SKU가 존재하지 않을 경우 오류를 반환합니다.
                """
    )
    @GetMapping("sku/{sku}")
    public ResponseEntity<BaseResponse<ItemVariantResponse>> getItemVariantBySku(
            @PathVariable String sku
    ) {
        ItemVariantResponse response = itemVariantService.findBySku(sku);
        return ResponseEntity.ok(BaseResponse.success("상품 단위 조회 성공", response));
    }

    @Operation(
            summary = "아이템별 상품 판매 단위 목록 조회",
            description = """
                특정 아이템에 속한 상품 판매 단위(ItemVariant) 목록을 조회합니다.
                
                [조회 조건]
                - itemId (필수): 아이템 ID
                - status (선택): 상품 판매 상태 (AVAILABLE, OUT_OF_STOCK, DISABLED)
                - minQuantity (선택): 최소 재고 수량 (기본값 0)
                
                [사용 예시]
                - 아이템의 모든 상품 단위 조회
                  → itemId=1
                  
                - 특정 상태의 상품 단위 조회
                  → itemId=1&status=AVAILABLE
                  
                - 판매 가능한 상품 단위 조회 (재고 있음)
                  → itemId=1&status=AVAILABLE&minQuantity=1
                """
    )
    @GetMapping
    public ResponseEntity<BaseResponse<List<ItemVariantResponse>>> getItemVariants(
            @RequestParam Long itemId,
            @RequestParam(required = false)ItemVariantStatus status,
            @RequestParam(required = false, defaultValue = "0") int minQuantity
            ) {
        List<ItemVariantResponse> response;
        if(status != null) {
            response = itemVariantService.findBuyableVariantsByItemId(itemId, status, minQuantity);
            return ResponseEntity.ok(BaseResponse.success("상품 단위 조회 성공", response));
        }
        response = itemVariantService.findByItemId(itemId);
        return ResponseEntity.ok(BaseResponse.success("상품 단위 조회 성공", response));
    }
}

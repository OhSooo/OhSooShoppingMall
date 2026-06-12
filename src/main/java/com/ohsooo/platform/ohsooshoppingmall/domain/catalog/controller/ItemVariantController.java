package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response.ItemVariantResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.service.ItemVariantService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalog/itemVariant")
@RequiredArgsConstructor
@Tag(name = "item variant", description = "상품 단위 조회 API")
public class ItemVariantController {

  private final ItemVariantService itemVariantService;

  @Operation(summary = "상품 단위 단건 조회 (ID)")
  @GetMapping("/{itemVariantId}")
  public ResponseEntity<BaseResponse<ItemVariantResponse>> getItemVariantById(
      @PathVariable Long itemVariantId
  ) {
    ItemVariantResponse response = itemVariantService.findById(itemVariantId);
    return ResponseEntity.ok(BaseResponse.success("상품 단위 조회 성공", response));
  }

  @Operation(summary = "상품 단위 단건 조회 (SKU)")
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
          특정 아이템에 속한 상품 판매 단위 목록을 조회합니다.
          - status 없이 조회하면 전체 반환
          - status=ACTIVE: 판매 가능한 단위만 반환 (재고 있음)
          - 재고 수량은 GET /inventory/item-variants/{itemVariantId} 로 별도 조회
          """
  )
  @GetMapping
  public ResponseEntity<BaseResponse<List<ItemVariantResponse>>> getItemVariants(
      @RequestParam Long itemId,
      @RequestParam(required = false) ItemVariantStatus status
  ) {
    List<ItemVariantResponse> response;
    if (status == ItemVariantStatus.ACTIVE) {
      response = itemVariantService.findSalableVariantsByItemId(itemId);
    } else if (status != null) {
      response = itemVariantService.findByItemIdAndStatus(itemId, status);
    } else {
      response = itemVariantService.findByItemId(itemId);
    }
    return ResponseEntity.ok(BaseResponse.success("상품 단위 조회 성공", response));
  }
}

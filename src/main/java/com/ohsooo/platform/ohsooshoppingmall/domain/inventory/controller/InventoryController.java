package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.controller;

import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.request.StockAdjustRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service.InventoryService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventory", description = "재고(Inventory) 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  @Operation(
      summary = "판매 단위 재고 조회",
      description = "itemVariantId 기준으로 현재 재고 수량과 상태를 조회합니다."
  )
  @GetMapping("/item-variants/{itemVariantId}")
  public ResponseEntity<BaseResponse<StockResponse>> getStock(
      @PathVariable Long itemVariantId
  ) {
    StockResponse response = inventoryService.getStock(itemVariantId);
    return ResponseEntity.ok(BaseResponse.success("재고 조회 성공", response));
  }

  @Operation(
      summary = "판매 단위 재고 변경(설정)",
      description = """
          itemVariantId 기준으로 재고 수량을 원하는 값으로 '설정'합니다.
          
          - 권한: OWNER/ADMIN (권한 체크는 추후 Security에서 적용)
          - quantity는 0 이상
          """
  )
  @PatchMapping("/item-variants/{itemVariantId}")
  public ResponseEntity<BaseResponse<StockResponse>> adjustStock(
      @PathVariable Long itemVariantId,
      @Valid @RequestBody StockAdjustRequest request
  ) {
    StockResponse response = inventoryService.adjustStock(itemVariantId, request);
    return ResponseEntity.ok(BaseResponse.success("재고 변경 성공", response));
  }
}

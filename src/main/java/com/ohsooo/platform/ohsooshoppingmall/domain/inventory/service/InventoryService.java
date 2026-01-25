package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.request.StockAdjustRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception.InventoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

  private final ItemVariantRepository itemVariantRepository;
  private final InventoryMapper inventoryMapper;

  /**
   * itemVariantId 기준으로 재고(수량/상태)를 조회한다.
   *
   * @param itemVariantId 판매 단위 ID
   * @return 재고 응답 DTO
   * @throws IllegalArgumentException 판매 단위가 존재하지 않는 경우
   */
  @Transactional(readOnly = true)
  public StockResponse getStock(Long itemVariantId) {
    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() ->
            new IllegalArgumentException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND.getMessage())
        );

    return inventoryMapper.toStockResponse(variant);
  }

  /**
   * itemVariantId 기준으로 재고 수량을 원하는 값으로 "설정"한다. (관리자/오너용)
   *
   * - quantity는 0 이상
   * - 판매중지(DISABLED) 상태는 재고 변경을 막는다(운영 정책)
   *
   * @param itemVariantId 판매 단위 ID
   * @param request 재고 설정 요청 DTO
   * @return 변경된 재고 응답 DTO
   * @throws IllegalArgumentException 판매 단위가 없거나, quantity가 유효하지 않거나, 판매중지 상태인 경우
   */
  @Transactional
  public StockResponse adjustStock(Long itemVariantId, StockAdjustRequest request) {
    Integer targetQuantity = request.getQuantity();
    if (targetQuantity == null || targetQuantity < 0) {
      throw new IllegalArgumentException(InventoryErrorCode.INVALID_STOCK_AMOUNT.getMessage());
    }

    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() ->
            new IllegalArgumentException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND.getMessage())
        );

    // 운영 정책: 판매중지 상태는 재고 설정도 막기
    if (variant.getStatus() == ItemVariantStatus.DISABLED) {
      throw new IllegalArgumentException(InventoryErrorCode.VARIANT_DISABLED.getMessage());
    }

    // setQuantity가 없어서 차이를 계산해 increase/decrease로 맞춘다.
    int current = variant.getQuantity();
    int target = targetQuantity;

    if (target > current) {
      variant.increaseQuantity(target - current);
    } else if (target < current) {
      variant.decreaseQuantity(current - target);
    }

    // quantity에 맞게 상태를 ACTIVE/OUT_OF_STOCK로 정리
    variant.enable();

    return inventoryMapper.toStockResponse(variant);
  }

  /**
   * 재고를 동시성 안전하게 차감한다. (결제 성공/주문 확정 시점에 사용)
   *
   * - Repository의 원자 UPDATE로 oversell을 방지한다.
   * - 차감 실패(수량 부족)면 예외를 발생시킨다.
   *
   * @param itemVariantId 판매 단위 ID
   * @param amount 차감할 수량(1 이상)
   * @return 차감 후 재고 응답 DTO
   * @throws IllegalArgumentException 판매 단위가 없거나, amount가 유효하지 않거나, 수량이 부족하거나, 판매중지 상태인 경우
   */
  @Transactional
  public StockResponse decreaseStock(Long itemVariantId, int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException(InventoryErrorCode.INVALID_STOCK_AMOUNT.getMessage());
    }

    // 상태 체크를 위해 한번 조회(판매중지/존재 여부)
    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() ->
            new IllegalArgumentException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND.getMessage())
        );

    if (variant.getStatus() == ItemVariantStatus.DISABLED) {
      throw new IllegalArgumentException(InventoryErrorCode.VARIANT_DISABLED.getMessage());
    }

    // 원자 차감: 성공하면 1, 실패하면 0
    int updated = itemVariantRepository.decreaseStockIfEnough(itemVariantId, amount);
    if (updated == 0) {
      // quantity가 부족하거나(=재고부족), 이미 0인 경우(=품절) -> MVP에선 재고 부족으로 통일
      throw new IllegalArgumentException(InventoryErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    // 변경된 값을 다시 조회해서 응답(정확한 quantity/status 반영)
    ItemVariant updatedVariant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() ->
            new IllegalArgumentException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND.getMessage())
        );

    // quantity가 0이면 OUT_OF_STOCK로 상태 정리(선택)
    // 지금은 enable()이 quantity에 맞춰 ACTIVE/OUT_OF_STOCK로 맞추니까 호출해줌
    if (updatedVariant.getStatus() != ItemVariantStatus.DISABLED) {
      updatedVariant.enable();
    }

    return inventoryMapper.toStockResponse(updatedVariant);
  }

  // TODO(주문/결제 연동)
  // - Payment 성공 콜백(or Order 확정) 시점에 decreaseStock(itemVariantId, qty)를 호출하도록 연결
  // - 여러 OrderItem을 한 번에 처리할 경우, 주문 서비스에서 itemVariantId/qty 목록을 순회하며 차감
  // - 차감 실패 시 주문 생성/결제 처리 롤백 정책 결정(전체 실패/부분 실패 등)
}

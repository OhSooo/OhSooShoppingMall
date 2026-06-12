package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.repository.ItemVariantRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.request.StockAdjustRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.entity.Inventory;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception.InventoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.mapper.InventoryMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.repository.InventoryRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

  private final InventoryRepository inventoryRepository;
  private final ItemVariantRepository itemVariantRepository;
  private final InventoryMapper inventoryMapper;

  /**
   * 재고 조회
   */
  @Transactional(readOnly = true)
  public StockResponse getStock(Long itemVariantId) {
    Inventory inventory = inventoryRepository.findByItemVariantId(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.INVENTORY_NOT_FOUND));

    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND));

    return inventoryMapper.toStockResponse(inventory, variant.getStatus());
  }

  /**
   * 재고 절대값 설정 (관리자/오너 전용).
   * 비관적 락으로 동시 요청 시 덮어쓰기 방지 (I-2 수정).
   */
  @Transactional
  public StockResponse adjustStock(Long itemVariantId, StockAdjustRequest request) {
    Integer targetQuantity = request.getQuantity();
    if (targetQuantity == null || targetQuantity < 0) {
      throw new BusinessException(InventoryErrorCode.INVALID_STOCK_AMOUNT);
    }

    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND));

    if (variant.getStatus() == ItemVariantStatus.DISABLED) {
      throw new BusinessException(InventoryErrorCode.VARIANT_DISABLED);
    }

    // 비관적 락으로 동시 조정 요청 직렬화
    Inventory inventory = inventoryRepository.findByItemVariantIdWithLock(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.INVENTORY_NOT_FOUND));

    inventory.setQuantity(targetQuantity);
    variant.syncStatus(targetQuantity);

    return inventoryMapper.toStockResponse(inventory, variant.getStatus());
  }

  /**
   * 재고 차감 (주문/결제 확정 시).
   * 원자 UPDATE로 oversell 방지. @Modifying의 clearAutomatically로 캐시가 클리어되므로
   * status 동기화는 별도 UPDATE 쿼리로 수행.
   */
  @Transactional
  public void decreaseStock(Long itemVariantId, int amount) {
    if (amount <= 0) {
      throw new BusinessException(InventoryErrorCode.INVALID_STOCK_AMOUNT);
    }

    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND));

    if (variant.getStatus() == ItemVariantStatus.DISABLED) {
      throw new BusinessException(InventoryErrorCode.VARIANT_DISABLED);
    }

    // 원자 차감: 재고 부족이면 0 반환
    int updated = inventoryRepository.decreaseStockIfEnough(itemVariantId, amount);
    if (updated == 0) {
      throw new BusinessException(InventoryErrorCode.INSUFFICIENT_STOCK);
    }

    // clearAutomatically로 L1 캐시가 클리어됐으므로 재조회 후 status 동기화
    Inventory fresh = inventoryRepository.findByItemVariantId(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.INVENTORY_NOT_FOUND));
    syncVariantStatus(itemVariantId, fresh.getQuantity());
  }

  /**
   * 재고 복원 (주문 취소 / 환불 시).
   */
  @Transactional
  public void increaseStock(Long itemVariantId, int amount) {
    if (amount <= 0) {
      throw new BusinessException(InventoryErrorCode.INVALID_STOCK_AMOUNT);
    }

    itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND));

    int updated = inventoryRepository.increaseStock(itemVariantId, amount);
    if (updated == 0) {
      throw new BusinessException(InventoryErrorCode.INVENTORY_NOT_FOUND);
    }

    Inventory fresh = inventoryRepository.findByItemVariantId(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.INVENTORY_NOT_FOUND));
    syncVariantStatus(itemVariantId, fresh.getQuantity());
  }

  /**
   * 신규 ItemVariant 등록 시 초기 재고 생성.
   * ItemVariant 생성 직후 호출해야 한다.
   */
  @Transactional
  public StockResponse createInventory(Long itemVariantId, int initialQuantity) {
    if (initialQuantity < 0) {
      throw new BusinessException(InventoryErrorCode.INVALID_STOCK_AMOUNT);
    }

    ItemVariant variant = itemVariantRepository.findById(itemVariantId)
        .orElseThrow(() -> new BusinessException(InventoryErrorCode.ITEM_VARIANT_NOT_FOUND));

    if (inventoryRepository.findByItemVariantId(itemVariantId).isPresent()) {
      throw new BusinessException(InventoryErrorCode.INVENTORY_ALREADY_EXISTS);
    }

    Inventory inventory = Inventory.of(itemVariantId, initialQuantity);
    inventoryRepository.save(inventory);

    variant.syncStatus(initialQuantity);

    return inventoryMapper.toStockResponse(inventory, variant.getStatus());
  }

  // status는 Inventory 수량 기준으로 결정되며, DISABLED 상태는 변경하지 않는다.
  private void syncVariantStatus(Long itemVariantId, int currentQuantity) {
    ItemVariantStatus newStatus = currentQuantity > 0
        ? ItemVariantStatus.ACTIVE
        : ItemVariantStatus.OUT_OF_STOCK;
    itemVariantRepository.syncStatus(itemVariantId, newStatus);
  }
}

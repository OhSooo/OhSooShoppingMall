package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariant;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

  /**
   * Catalog의 ItemVariant 엔티티를 Inventory 도메인의 StockResponse로 변환한다.
   *
   * @param itemVariant 상품 판매 단위 엔티티
   * @return 재고 응답 DTO
   */
  public StockResponse toStockResponse(ItemVariant itemVariant) {
    return new StockResponse(
        itemVariant.getItemVariantId(),
        itemVariant.getQuantity(),
        itemVariant.getStatus()
    );
  }
}

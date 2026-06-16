package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response.StockResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

  public StockResponse toStockResponse(Inventory inventory, ItemVariantStatus status) {
    return new StockResponse(
        inventory.getItemVariantId(),
        inventory.getQuantity(),
        status
    );
  }
}

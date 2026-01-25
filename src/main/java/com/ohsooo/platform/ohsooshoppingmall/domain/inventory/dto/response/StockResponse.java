package com.ohsooo.platform.ohsooshoppingmall.domain.inventory.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockResponse {

  private Long itemVariantId;
  private int quantity;
  private ItemVariantStatus status;
}

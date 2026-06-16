package com.ohsooo.platform.ohsooshoppingmall.domain.catalog.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.catalog.entity.variant.ItemVariantStatus;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class ItemVariantResponse {

  private final Long id;

  private final Long itemId;
  private final String itemName;

  private final String sku;
  private final BigDecimal price;
  private final ItemVariantStatus status;

  public ItemVariantResponse(
      Long id,
      Long itemId,
      String itemName,
      String sku,
      BigDecimal price,
      ItemVariantStatus status
  ) {
    this.id = id;
    this.itemId = itemId;
    this.itemName = itemName;
    this.sku = sku;
    this.price = price;
    this.status = status;
  }
}

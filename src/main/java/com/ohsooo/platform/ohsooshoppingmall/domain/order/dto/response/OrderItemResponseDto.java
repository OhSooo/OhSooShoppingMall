package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemResponseDto {

  private Long orderItemId;

  private Long itemId;
  private Long itemVariantId;
  private String sku;

  private String itemName;

  /** 구매 당시 옵션 조합 스냅샷 (예: "블랙/L") */
  private String optionSummary;

  /** 구매 당시 가격 스냅샷 */
  private BigDecimal priceAtPurchase;

  private int quantity;

  private List<OrderItemOptionResponseDto> options;

  private String status;

  private boolean saleable;
  private boolean cancelable;
}

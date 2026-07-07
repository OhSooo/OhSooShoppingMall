package com.ohsooo.platform.ohsooshoppingmall.domain.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CartMergeSkippedItemDto {

  private Long itemVariantId;
  private String errorCode;
  private String reason;
}

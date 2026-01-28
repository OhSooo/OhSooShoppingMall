package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCreateRequestDto {

  /**
   * 구매할 상품 단위(판매 단위)
   */
  @NotNull
  private Long itemVariantId;

  /**
   * 구매 수량
   */
  @NotNull
  @Positive
  private Integer quantity;
}

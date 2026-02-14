package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCreateResponseDto {

  private Long orderId;
  private String status;

  /** 총 결제 예정 금액 */
  private BigDecimal totalPrice;

  private List<OrderItemResponseDto> items;
  private String paymentRedirectHint;
}

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

  private BigDecimal originalTotalPrice;
  private BigDecimal discountAmount;
  private BigDecimal deliveryFee;
  private BigDecimal finalPrice;

  private List<OrderItemResponseDto> items;
  private String paymentRedirectHint;
}

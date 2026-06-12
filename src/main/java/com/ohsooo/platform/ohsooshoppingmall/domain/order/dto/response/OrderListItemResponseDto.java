package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderListItemResponseDto {

  private Long orderId;
  private String status;

  private BigDecimal finalPrice;

  private OffsetDateTime createdAt;
  private String summary;
}

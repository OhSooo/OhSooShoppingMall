package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderListItemResponseDto {

  private Long orderId;

  private String status;

  private int totalPrice;

  private OffsetDateTime createdAt;

  /**
   * 목록에서 미리보기로 필요한 정도만
   * 예: "외 2건"
   */
  private String summary;
}

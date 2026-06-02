package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderItemOptionResponseDto {

  /** "SIZE" / "COLOR" 같은 문자열 */
  private String type;

  /** 옵션 값 (예: "M", "Black") */
  private String value;
}

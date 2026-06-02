package com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCancelRequestDto {

  /**
   * 취소 사유 (사용자 입력 or 사유 코드로 확장 가능)
   */
  @NotBlank
  private String reason;
}

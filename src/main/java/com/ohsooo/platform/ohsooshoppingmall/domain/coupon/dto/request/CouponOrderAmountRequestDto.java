package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponOrderAmountRequestDto {

  @NotNull
  @DecimalMin(value = "0.0", inclusive = false)
  private BigDecimal orderAmount;

  /** 쿠폰을 사용하는 주문 ID (선택, 사용 처리 시점에 없을 수도 있음) */
  private Long orderId;
}

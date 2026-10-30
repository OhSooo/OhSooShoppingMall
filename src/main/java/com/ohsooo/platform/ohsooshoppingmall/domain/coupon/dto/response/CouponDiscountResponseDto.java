package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponDiscountResponseDto {

  private Long userCouponId;
  private BigDecimal orderAmount;
  private BigDecimal discountAmount;
  private BigDecimal finalAmount;
}

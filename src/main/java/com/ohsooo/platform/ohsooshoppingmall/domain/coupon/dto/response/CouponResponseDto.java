package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponResponseDto {

  private Long couponId;
  private String name;
  private DiscountType discountType;
  private BigDecimal discountValue;
  private BigDecimal minOrderAmount;
  private BigDecimal maxDiscountAmount;
  private OffsetDateTime issueStartAt;
  private OffsetDateTime issueEndAt;
  private OffsetDateTime useStartAt;
  private OffsetDateTime useEndAt;
}

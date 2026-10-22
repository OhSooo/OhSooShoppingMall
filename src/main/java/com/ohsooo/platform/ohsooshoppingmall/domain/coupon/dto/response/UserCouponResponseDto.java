package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCouponResponseDto {

  private Long userCouponId;
  private CouponResponseDto coupon;
  private UserCouponStatus status;
  private OffsetDateTime issuedAt;
  private OffsetDateTime usedAt;
  private BigDecimal discountAppliedAmount;
}

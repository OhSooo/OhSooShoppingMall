package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponDiscountResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

  public CouponResponseDto toCouponResponseDto(Coupon coupon) {
    return new CouponResponseDto(
        coupon.getCouponId(),
        coupon.getName(),
        coupon.getDiscountType(),
        coupon.getDiscountValue(),
        coupon.getMinOrderAmount(),
        coupon.getMaxDiscountAmount(),
        coupon.getIssueStartAt(),
        coupon.getIssueEndAt(),
        coupon.getUseStartAt(),
        coupon.getUseEndAt()
    );
  }

  public UserCouponResponseDto toUserCouponResponseDto(UserCoupon userCoupon, OffsetDateTime now) {
    return new UserCouponResponseDto(
        userCoupon.getUserCouponId(),
        toCouponResponseDto(userCoupon.getCoupon()),
        userCoupon.effectiveStatus(now),
        userCoupon.getIssuedAt(),
        userCoupon.getUsedAt(),
        userCoupon.getDiscountAppliedAmount()
    );
  }

  public CouponDiscountResponseDto toCouponDiscountResponseDto(
      UserCoupon userCoupon, BigDecimal orderAmount, BigDecimal discountAmount
  ) {
    return new CouponDiscountResponseDto(
        userCoupon.getUserCouponId(),
        orderAmount,
        discountAmount,
        orderAmount.subtract(discountAmount)
    );
  }
}

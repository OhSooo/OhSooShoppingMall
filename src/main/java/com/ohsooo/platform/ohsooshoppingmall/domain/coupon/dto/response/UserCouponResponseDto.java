package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCouponResponseDto {

  private Long userCouponId;
  private CouponResponseDto coupon;

  @Schema(description = "조회 시점 기준 상태. 사용 시작 전인 미사용 쿠폰도 AVAILABLE이다.", example = "AVAILABLE")
  private UserCouponStatus status;

  @Schema(description = "조회 시점에 사용 기간 안에 있는 미사용 쿠폰인지 여부 (최소 주문 금액 조건은 제외)", example = "true")
  private boolean usableNow;

  private OffsetDateTime issuedAt;
  private OffsetDateTime usedAt;
  private BigDecimal discountAppliedAmount;
}

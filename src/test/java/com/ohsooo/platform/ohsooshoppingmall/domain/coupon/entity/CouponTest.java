package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CouponTest {

  private static final OffsetDateTime NOW = OffsetDateTime.now();

  private Coupon fixedCoupon(BigDecimal discountValue) {
    return Coupon.create(
        "정액쿠폰", DiscountType.FIXED_AMOUNT, discountValue, null, null,
        NOW.minusDays(1), NOW.plusDays(1), NOW.minusDays(1), NOW.plusDays(1)
    );
  }

  private Coupon percentageCoupon(BigDecimal discountValue, BigDecimal maxDiscountAmount) {
    return Coupon.create(
        "정률쿠폰", DiscountType.PERCENTAGE, discountValue, null, maxDiscountAmount,
        NOW.minusDays(1), NOW.plusDays(1), NOW.minusDays(1), NOW.plusDays(1)
    );
  }

  @Test
  void 정액_할인액이_주문금액보다_크면_주문금액으로_캡된다() {
    Coupon coupon = fixedCoupon(BigDecimal.valueOf(10000));

    BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(3000));

    assertThat(discount).isEqualByComparingTo("3000");
  }

  @Test
  void 정률_할인은_최대할인금액을_초과하지_않는다() {
    // 10000원 주문에 50% 할인이면 5000원이지만 maxDiscountAmount=2000으로 캡되어야 한다
    Coupon coupon = percentageCoupon(BigDecimal.valueOf(50), BigDecimal.valueOf(2000));

    BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(10000));

    assertThat(discount).isEqualByComparingTo("2000");
  }

  @Test
  void 최소주문금액_미충족이면_false를_반환한다() {
    Coupon coupon = Coupon.create(
        "최소주문쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000),
        BigDecimal.valueOf(10000), null,
        NOW.minusDays(1), NOW.plusDays(1), NOW.minusDays(1), NOW.plusDays(1)
    );

    assertThat(coupon.meetsMinOrderAmount(BigDecimal.valueOf(5000))).isFalse();
    assertThat(coupon.meetsMinOrderAmount(BigDecimal.valueOf(10000))).isTrue();
  }

  @Test
  void 발급기간_이전이면_발급불가() {
    Coupon coupon = fixedCouponWithIssuePeriod(NOW.plusDays(1), NOW.plusDays(2));

    assertThat(coupon.isIssuableAt(NOW)).isFalse();
  }

  @Test
  void 발급기간_이후이면_발급불가() {
    Coupon coupon = fixedCouponWithIssuePeriod(NOW.minusDays(2), NOW.minusDays(1));

    assertThat(coupon.isIssuableAt(NOW)).isFalse();
  }

  private Coupon fixedCouponWithIssuePeriod(OffsetDateTime issueStartAt, OffsetDateTime issueEndAt) {
    return Coupon.create(
        "발급기간쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        issueStartAt, issueEndAt, NOW.minusDays(1), NOW.plusDays(1)
    );
  }
}

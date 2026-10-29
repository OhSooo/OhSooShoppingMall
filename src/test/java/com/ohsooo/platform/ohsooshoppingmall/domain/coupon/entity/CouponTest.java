package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
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

  // ---------- 원 단위 할인 계산 ----------

  @Test
  void 정률_할인액은_원_미만을_절사한다() {
    // 10999 * 15% = 1649.85 → 1649
    Coupon coupon = percentageCoupon(BigDecimal.valueOf(15), null);

    BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(10999));

    assertThat(discount).isEqualByComparingTo("1649");
    assertThat(Coupon.isWholeWon(discount)).isTrue();
  }

  @Test
  void 소수점_정률도_할인액은_정수_원이다() {
    // 10001 * 12.5% = 1250.125 → 1250
    Coupon coupon = percentageCoupon(new BigDecimal("12.5"), null);

    BigDecimal discount = coupon.calculateDiscount(BigDecimal.valueOf(10001));

    assertThat(discount).isEqualByComparingTo("1250");
    assertThat(Coupon.isWholeWon(discount)).isTrue();
  }

  @Test
  void 절사한_정률_할인액에도_최대할인금액이_적용된다() {
    Coupon coupon = percentageCoupon(BigDecimal.valueOf(15), BigDecimal.valueOf(1000));

    assertThat(coupon.calculateDiscount(BigDecimal.valueOf(10999))).isEqualByComparingTo("1000");
  }

  @Test
  void 정률_100퍼센트_할인이면_최종금액이_0원이_된다() {
    Coupon coupon = percentageCoupon(BigDecimal.valueOf(100), null);
    BigDecimal orderAmount = BigDecimal.valueOf(10999);

    BigDecimal discount = coupon.calculateDiscount(orderAmount);

    assertThat(discount).isEqualByComparingTo("10999");
    assertThat(orderAmount.subtract(discount)).isEqualByComparingTo("0");
  }

  @Test
  void 최대할인금액이_주문금액보다_커도_할인액은_주문금액을_넘지_않는다() {
    Coupon coupon = percentageCoupon(BigDecimal.valueOf(100), BigDecimal.valueOf(50000));

    assertThat(coupon.calculateDiscount(BigDecimal.valueOf(3000))).isEqualByComparingTo("3000");
  }

  // ---------- 금액 정책 검증 ----------

  @Test
  void 정액_할인금액에_원_미만_단위가_있으면_생성이_거부된다() {
    assertThatThrownBy(() -> fixedCoupon(new BigDecimal("1000.50")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.INVALID_DISCOUNT_POLICY);
  }

  @Test
  void 정액_할인금액이_소수점_0이면_정수로_보고_허용한다() {
    Coupon coupon = fixedCoupon(new BigDecimal("1000.00"));

    assertThat(coupon.calculateDiscount(BigDecimal.valueOf(5000))).isEqualByComparingTo("1000");
  }

  @Test
  void 최소주문금액에_원_미만_단위가_있으면_생성이_거부된다() {
    assertThatThrownBy(() -> Coupon.create(
        "쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), new BigDecimal("9999.99"), null,
        NOW.minusDays(1), NOW.plusDays(1), NOW.minusDays(1), NOW.plusDays(1)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.INVALID_DISCOUNT_POLICY);
  }

  @Test
  void 최대할인금액에_원_미만_단위가_있으면_생성이_거부된다() {
    assertThatThrownBy(() -> percentageCoupon(BigDecimal.valueOf(10), new BigDecimal("2000.10")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.INVALID_DISCOUNT_POLICY);
  }

  private Coupon fixedCouponWithIssuePeriod(OffsetDateTime issueStartAt, OffsetDateTime issueEndAt) {
    return Coupon.create(
        "발급기간쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        issueStartAt, issueEndAt, NOW.minusDays(1), NOW.plusDays(1)
    );
  }
}

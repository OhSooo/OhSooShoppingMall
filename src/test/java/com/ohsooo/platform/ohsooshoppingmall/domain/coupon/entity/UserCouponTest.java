package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserCouponTest {

  private static final OffsetDateTime NOW = OffsetDateTime.now();
  private static final OffsetDateTime USE_START = NOW.minusDays(1);
  private static final OffsetDateTime USE_END = NOW.plusDays(1);

  private Coupon coupon() {
    return Coupon.create(
        "쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), null,
        NOW.minusDays(2), NOW.plusDays(2), USE_START, USE_END
    );
  }

  private UserCoupon availableUserCoupon() {
    return UserCoupon.issue(Mockito.mock(User.class), coupon(), NOW);
  }

  @Test
  void 이미_사용된_쿠폰을_다시_사용처리하면_거부되고_기존_이력이_유지된다() {
    UserCoupon userCoupon = availableUserCoupon();
    userCoupon.markUsed(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), 1L, NOW);

    assertThatThrownBy(() -> userCoupon.markUsed(BigDecimal.valueOf(20000), BigDecimal.valueOf(1000), 2L, NOW))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ALREADY_USED);
    assertThat(userCoupon.getUsedOrderId()).isEqualTo(1L);
    assertThat(userCoupon.getUsedOrderAmount()).isEqualByComparingTo("10000");
  }

  @Test
  void 복구하면_AVAILABLE이_되고_사용이력_필드가_초기화된다() {
    UserCoupon userCoupon = availableUserCoupon();
    userCoupon.markUsed(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), 1L, NOW);

    userCoupon.restore();

    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(userCoupon.getUsedOrderId()).isNull();
    assertThat(userCoupon.getUsedOrderAmount()).isNull();
    assertThat(userCoupon.getDiscountAppliedAmount()).isNull();
    assertThat(userCoupon.getUsedAt()).isNull();
  }

  @Test
  void 복구해도_쿠폰의_사용기간과_사용조건은_그대로다() {
    UserCoupon userCoupon = availableUserCoupon();
    OffsetDateTime issuedAt = userCoupon.getIssuedAt();
    userCoupon.markUsed(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), 1L, NOW);

    userCoupon.restore();

    Coupon coupon = userCoupon.getCoupon();
    assertThat(coupon.getUseStartAt()).isEqualTo(USE_START);
    assertThat(coupon.getUseEndAt()).isEqualTo(USE_END);
    assertThat(coupon.getMinOrderAmount()).isEqualByComparingTo("5000");
    assertThat(coupon.getDiscountValue()).isEqualByComparingTo("1000");
    assertThat(userCoupon.getIssuedAt()).isEqualTo(issuedAt);
  }

  @Test
  void 복구된_쿠폰은_다시_사용처리할_수_있다() {
    UserCoupon userCoupon = availableUserCoupon();
    userCoupon.markUsed(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), 1L, NOW);
    userCoupon.restore();

    userCoupon.markUsed(BigDecimal.valueOf(8000), BigDecimal.valueOf(1000), 2L, NOW);

    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
    assertThat(userCoupon.getUsedOrderId()).isEqualTo(2L);
  }

  @Test
  void 사용되지_않은_쿠폰은_복구할_수_없다() {
    UserCoupon userCoupon = availableUserCoupon();

    assertThatThrownBy(userCoupon::restore)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_NOT_RESTORABLE);
  }

  @Test
  void 같은_쿠폰을_두번_복구하면_두번째는_거부된다() {
    UserCoupon userCoupon = availableUserCoupon();
    userCoupon.markUsed(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), 1L, NOW);
    userCoupon.restore();

    assertThatThrownBy(userCoupon::restore)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_NOT_RESTORABLE);
  }

  // ---------- 조회 시점 상태 / 지금 사용 가능 여부 ----------

  private UserCoupon userCouponWithUsePeriod(OffsetDateTime useStartAt, OffsetDateTime useEndAt) {
    Coupon coupon = Coupon.create(
        "쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.minusDays(10), NOW.plusDays(10), useStartAt, useEndAt
    );
    return UserCoupon.issue(Mockito.mock(User.class), coupon, NOW);
  }

  @Test
  void 사용기간_안의_미사용_쿠폰은_AVAILABLE이고_지금_사용가능하다() {
    UserCoupon userCoupon = userCouponWithUsePeriod(NOW.minusDays(1), NOW.plusDays(1));

    assertThat(userCoupon.effectiveStatus(NOW)).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(userCoupon.isUsableAt(NOW)).isTrue();
  }

  @Test
  void 사용시작_전_쿠폰은_AVAILABLE이지만_지금_사용가능하지_않다() {
    UserCoupon userCoupon = userCouponWithUsePeriod(NOW.plusDays(1), NOW.plusDays(2));

    assertThat(userCoupon.effectiveStatus(NOW)).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(userCoupon.isUsableAt(NOW)).isFalse();
  }

  @Test
  void 사용종료가_지난_미사용_쿠폰은_EXPIRED이고_사용가능하지_않다() {
    UserCoupon userCoupon = userCouponWithUsePeriod(NOW.minusDays(2), NOW.minusDays(1));

    assertThat(userCoupon.effectiveStatus(NOW)).isEqualTo(UserCouponStatus.EXPIRED);
    assertThat(userCoupon.isUsableAt(NOW)).isFalse();
  }

  @Test
  void 사용종료_시각_정각에는_아직_사용가능하다() {
    OffsetDateTime useEnd = NOW.plusDays(1);
    UserCoupon userCoupon = userCouponWithUsePeriod(NOW.minusDays(1), useEnd);

    assertThat(userCoupon.effectiveStatus(useEnd)).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(userCoupon.isUsableAt(useEnd)).isTrue();
  }

  @Test
  void 사용한_쿠폰은_USED이고_사용가능하지_않다() {
    UserCoupon userCoupon = availableUserCoupon();
    userCoupon.markUsed(BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), 1L, NOW);

    assertThat(userCoupon.effectiveStatus(NOW)).isEqualTo(UserCouponStatus.USED);
    assertThat(userCoupon.isUsableAt(NOW)).isFalse();
  }
}

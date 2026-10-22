package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponDiscountResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper.CouponMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.UserCouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCouponServiceTest {

  private static final OffsetDateTime NOW = OffsetDateTime.now();
  private static final Long OWNER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;

  @Mock
  private UserCouponRepository userCouponRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CouponService couponService;

  private final CouponMapper couponMapper = new CouponMapper();

  private UserCouponService userCouponService;

  private UserCouponService service() {
    return new UserCouponService(userCouponRepository, userRepository, couponService, couponMapper);
  }

  private User mockUser(Long userId) {
    User user = org.mockito.Mockito.mock(User.class);
    org.mockito.Mockito.lenient().when(user.getUserId()).thenReturn(userId);
    return user;
  }

  private Coupon coupon(
      DiscountType type, BigDecimal discountValue, BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
      OffsetDateTime issueStartAt, OffsetDateTime issueEndAt,
      OffsetDateTime useStartAt, OffsetDateTime useEndAt
  ) {
    return Coupon.create(
        "테스트쿠폰", type, discountValue, minOrderAmount, maxDiscountAmount,
        issueStartAt, issueEndAt, useStartAt, useEndAt
    );
  }

  private Coupon usableNowCoupon() {
    return coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(3000), null, null,
        NOW.minusDays(2), NOW.plusDays(2), NOW.minusDays(1), NOW.plusDays(1)
    );
  }

  // ---------- 발급 ----------

  @Test
  void 발급시작_이전이면_발급이_거부된다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.plusDays(1), NOW.plusDays(2), NOW.minusDays(1), NOW.plusDays(1)
    );
    when(couponService.getCouponEntity(10L)).thenReturn(coupon);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_ISSUE_PERIOD_INVALID);
  }

  @Test
  void 발급종료_이후이면_발급이_거부된다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.minusDays(2), NOW.minusDays(1), NOW.minusDays(1), NOW.plusDays(1)
    );
    when(couponService.getCouponEntity(10L)).thenReturn(coupon);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_ISSUE_PERIOD_INVALID);
  }

  @Test
  void 동일사용자가_동일쿠폰을_중복발급받으면_거부된다() {
    userCouponService = service();
    Coupon coupon = usableNowCoupon();
    when(couponService.getCouponEntity(10L)).thenReturn(coupon);
    when(userCouponRepository.existsByUser_UserIdAndCoupon_CouponId(OWNER_ID, 10L)).thenReturn(true);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_ALREADY_ISSUED);
  }

  @Test
  void 발급기간_내이고_미발급_상태면_정상_발급된다() {
    userCouponService = service();
    Coupon coupon = usableNowCoupon();
    User user = mockUser(OWNER_ID);
    when(couponService.getCouponEntity(10L)).thenReturn(coupon);
    when(userCouponRepository.existsByUser_UserIdAndCoupon_CouponId(OWNER_ID, 10L)).thenReturn(false);
    when(userRepository.findByUserIdAndIsDeletedFalse(OWNER_ID)).thenReturn(Optional.of(user));

    UserCouponResponseDto response = userCouponService.issueCoupon(OWNER_ID, 10L);

    assertThat(response.getStatus().name()).isEqualTo("AVAILABLE");
  }

  // ---------- 사용 검증/처리 ----------

  @Test
  void 정액할인이_주문금액보다_크면_주문금액만큼만_할인된다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(10000), null, null,
        NOW.minusDays(2), NOW.plusDays(2), NOW.minusDays(1), NOW.plusDays(1)
    );
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    CouponDiscountResponseDto response =
        userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(3000), null);

    assertThat(response.getDiscountAmount()).isEqualByComparingTo("3000");
    assertThat(response.getFinalAmount()).isEqualByComparingTo("0");
  }

  @Test
  void 정률할인은_최대할인금액을_초과하지_않는다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.PERCENTAGE, BigDecimal.valueOf(50), null, BigDecimal.valueOf(2000),
        NOW.minusDays(2), NOW.plusDays(2), NOW.minusDays(1), NOW.plusDays(1)
    );
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    CouponDiscountResponseDto response =
        userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(10000), null);

    assertThat(response.getDiscountAmount()).isEqualByComparingTo("2000");
  }

  @Test
  void 최소주문금액_미충족이면_사용이_거부된다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), BigDecimal.valueOf(10000), null,
        NOW.minusDays(2), NOW.plusDays(2), NOW.minusDays(1), NOW.plusDays(1)
    );
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
  }

  @Test
  void 사용시작_이전이면_사용이_거부된다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.minusDays(2), NOW.plusDays(2), NOW.plusDays(1), NOW.plusDays(2)
    );
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_NOT_YET_USABLE);
  }

  @Test
  void 사용종료_이후이면_사용이_거부된다() {
    userCouponService = service();
    Coupon coupon = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.minusDays(3), NOW.minusDays(2), NOW.minusDays(2), NOW.minusDays(1)
    );
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW.minusDays(3));
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_EXPIRED);
  }

  @Test
  void 이미_사용된_쿠폰은_재사용이_거부된다() {
    userCouponService = service();
    Coupon coupon = usableNowCoupon();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    userCoupon.markUsed(BigDecimal.valueOf(1000), BigDecimal.valueOf(500), null, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ALREADY_USED);
  }

  @Test
  void 존재하지_않는_유저쿠폰을_사용하면_거부된다() {
    userCouponService = service();
    when(userCouponRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userCouponService.useCoupon(OWNER_ID, 999L, BigDecimal.valueOf(5000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_NOT_FOUND);
  }

  @Test
  void 다른_사용자가_소유한_쿠폰은_사용할_수_없다() {
    userCouponService = service();
    Coupon coupon = usableNowCoupon();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(
        () -> userCouponService.useCoupon(OTHER_USER_ID, 100L, BigDecimal.valueOf(1000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ACCESS_DENIED);
  }

  @Test
  void 다른_사용자가_소유한_쿠폰은_미리보기도_할_수_없다() {
    userCouponService = service();
    Coupon coupon = usableNowCoupon();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(
        () -> userCouponService.previewDiscount(OTHER_USER_ID, 100L, BigDecimal.valueOf(1000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ACCESS_DENIED);
  }

  @Test
  void 소유자_본인은_정상적으로_사용할_수_있다() {
    userCouponService = service();
    Coupon coupon = usableNowCoupon();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), coupon, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    CouponDiscountResponseDto response =
        userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null);

    assertThat(response.getDiscountAmount()).isEqualByComparingTo("3000");
    assertThat(userCoupon.getStatus().name()).isEqualTo("USED");
  }
}

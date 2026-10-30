package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.UserCouponSearchRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponDiscountResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponPageResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper.CouponMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.UserCouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    verify(userCouponRepository).save(any(UserCoupon.class));
  }

  // ---------- 발급: DB 제약조건 위반 변환 ----------

  private void stubIssuablePath() {
    User user = mockUser(OWNER_ID);
    when(couponService.getCouponEntity(10L)).thenReturn(usableNowCoupon());
    when(userCouponRepository.existsByUser_UserIdAndCoupon_CouponId(OWNER_ID, 10L)).thenReturn(false);
    when(userRepository.findByUserIdAndIsDeletedFalse(OWNER_ID)).thenReturn(Optional.of(user));
  }

  // 실제 실행 시 예외 구조: DataIntegrityViolationException → Hibernate ConstraintViolationException → PSQLException.
  // 한국어 DB 메시지에서는 Hibernate가 제약조건 이름을 추출하지 못하므로(null) 그 상황을 그대로 재현한다.
  private DataIntegrityViolationException integrityViolation(String sqlState, String constraintName) {
    StringBuilder fields = new StringBuilder()
        .append("SERROR\0")
        .append("C").append(sqlState).append("\0")
        .append("M제약 조건을 위반함\0");
    if (constraintName != null) {
      fields.append("n").append(constraintName).append("\0");
    }
    return wrap(new PSQLException(new ServerErrorMessage(fields.toString())));
  }

  private DataIntegrityViolationException wrap(PSQLException psqlException) {
    ConstraintViolationException hibernateException =
        new ConstraintViolationException("could not execute statement", psqlException, null);
    return new DataIntegrityViolationException("could not execute statement", hibernateException);
  }

  @Test
  void 동시발급으로_uq_user_coupon_제약을_위반하면_중복발급_예외로_변환된다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation = integrityViolation("23505", "uq_user_coupon");
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L))
        .isInstanceOf(BusinessException.class)
        .hasCause(violation)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_ALREADY_ISSUED);
    assertThat(CouponErrorCode.COUPON_ALREADY_ISSUED.getStatus().value()).isEqualTo(409);
  }

  @Test
  void FK_제약조건_위반은_중복발급_예외로_변환하지_않는다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation = integrityViolation("23503", "user_coupons_user_id_fkey");
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L)).isSameAs(violation);
  }

  @Test
  void NOT_NULL_제약조건_위반은_중복발급_예외로_변환하지_않는다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation = integrityViolation("23502", null);
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L)).isSameAs(violation);
  }

  @Test
  void 다른_UNIQUE_제약조건_위반은_중복발급_예외로_변환하지_않는다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation = integrityViolation("23505", "user_coupons_pkey");
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L)).isSameAs(violation);
  }

  @Test
  void 서버_오류정보가_없는_PSQLException은_중복발급_예외로_변환하지_않는다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation =
        wrap(new PSQLException("duplicate", PSQLState.UNIQUE_VIOLATION));
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L)).isSameAs(violation);
  }

  @Test
  void UNIQUE_위반이지만_제약조건_이름이_없으면_중복발급_예외로_변환하지_않는다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation = integrityViolation("23505", null);
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L)).isSameAs(violation);
  }

  @Test
  void PSQLException_원인정보가_없는_무결성_예외는_그대로_전파된다() {
    userCouponService = service();
    stubIssuablePath();
    DataIntegrityViolationException violation = new DataIntegrityViolationException("unknown");
    when(userCouponRepository.save(any(UserCoupon.class))).thenThrow(violation);

    assertThatThrownBy(() -> userCouponService.issueCoupon(OWNER_ID, 10L)).isSameAs(violation);
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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ALREADY_USED);
  }

  @Test
  void 존재하지_않는_유저쿠폰을_사용하면_거부된다() {
    userCouponService = service();
    when(userCouponRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

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
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

    CouponDiscountResponseDto response =
        userCouponService.useCoupon(OWNER_ID, 100L, BigDecimal.valueOf(5000), null);

    assertThat(response.getDiscountAmount()).isEqualByComparingTo("3000");
    assertThat(userCoupon.getStatus().name()).isEqualTo("USED");
  }

  // ---------- 주문용 할인 계산 ----------

  private Coupon percentageCoupon(BigDecimal percent, BigDecimal maxDiscountAmount) {
    return coupon(
        DiscountType.PERCENTAGE, percent, null, maxDiscountAmount,
        NOW.minusDays(2), NOW.plusDays(2), NOW.minusDays(1), NOW.plusDays(1)
    );
  }

  @Test
  void 주문용_할인계산은_미리보기와_같은_금액을_반환한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), percentageCoupon(BigDecimal.valueOf(15), null), NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));
    BigDecimal orderAmount = BigDecimal.valueOf(10999);

    BigDecimal discount = userCouponService.calculateDiscount(OWNER_ID, 100L, orderAmount);
    CouponDiscountResponseDto preview = userCouponService.previewDiscount(OWNER_ID, 100L, orderAmount);

    assertThat(discount).isEqualByComparingTo("1649");
    assertThat(preview.getDiscountAmount()).isEqualByComparingTo(discount);
  }

  @Test
  void 주문용_할인계산은_쿠폰_상태를_바꾸지_않고_락도_잡지_않는다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    userCouponService.calculateDiscount(OWNER_ID, 100L, BigDecimal.valueOf(5000));

    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(userCoupon.getUsedOrderId()).isNull();
    verify(userCouponRepository, never()).findByIdWithLock(any());
  }

  @Test
  void 주문용_할인계산은_타인_쿠폰을_거부한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.calculateDiscount(OTHER_USER_ID, 100L, BigDecimal.valueOf(5000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ACCESS_DENIED);
  }

  @Test
  void 주문용_할인계산은_이미_사용된_쿠폰을_거부한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    userCoupon.markUsed(BigDecimal.valueOf(5000), BigDecimal.valueOf(3000), 1L, NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.calculateDiscount(OWNER_ID, 100L, BigDecimal.valueOf(5000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ALREADY_USED);
  }

  @Test
  void 원_미만_단위가_있는_주문금액은_거부된다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.calculateDiscount(OWNER_ID, 100L, new BigDecimal("5000.50")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.INVALID_ORDER_AMOUNT);
  }

  // ---------- 주문용 실제 사용(redeem) ----------

  @Test
  void redeem은_락으로_조회한_쿠폰을_USED로_바꾸고_사용이력을_기록한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), percentageCoupon(BigDecimal.valueOf(15), null), NOW);
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

    BigDecimal applied = userCouponService.redeem(
        OWNER_ID, 100L, 77L, BigDecimal.valueOf(10999), BigDecimal.valueOf(1649));

    assertThat(applied).isEqualByComparingTo("1649");
    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
    assertThat(userCoupon.getUsedOrderId()).isEqualTo(77L);
    assertThat(userCoupon.getUsedOrderAmount()).isEqualByComparingTo("10999");
    assertThat(userCoupon.getDiscountAppliedAmount()).isEqualByComparingTo("1649");
    assertThat(userCoupon.getUsedAt()).isNotNull();
    verify(userCouponRepository, never()).findById(any());
  }

  @Test
  void redeem시_재계산한_할인액이_예상과_다르면_거부되고_쿠폰은_그대로다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.redeem(
        OWNER_ID, 100L, 77L, BigDecimal.valueOf(5000), BigDecimal.valueOf(2000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_DISCOUNT_MISMATCH);
    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
  }

  @Test
  void redeem은_이미_사용된_쿠폰을_거부하고_기존_사용이력을_유지한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    userCoupon.markUsed(BigDecimal.valueOf(5000), BigDecimal.valueOf(3000), 1L, NOW);
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.redeem(
        OWNER_ID, 100L, 2L, BigDecimal.valueOf(5000), BigDecimal.valueOf(3000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ALREADY_USED);
    assertThat(userCoupon.getUsedOrderId()).isEqualTo(1L);
  }

  @Test
  void redeem은_타인_쿠폰을_거부한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    when(userCouponRepository.findByIdWithLock(100L)).thenReturn(Optional.of(userCoupon));

    assertThatThrownBy(() -> userCouponService.redeem(
        OTHER_USER_ID, 100L, 77L, BigDecimal.valueOf(5000), BigDecimal.valueOf(3000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ACCESS_DENIED);
    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
  }

  @Test
  void redeem은_주문ID가_없으면_거부된다() {
    userCouponService = service();

    assertThatThrownBy(() -> userCouponService.redeem(
        OWNER_ID, 100L, null, BigDecimal.valueOf(5000), BigDecimal.valueOf(3000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.INVALID_ORDER_ID);
    verify(userCouponRepository, never()).findByIdWithLock(any());
  }

  // ---------- 주문 취소/환불 시 복구 ----------

  @Test
  void releaseByOrder는_해당_주문에_사용된_쿠폰을_복구한다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    userCoupon.markUsed(BigDecimal.valueOf(5000), BigDecimal.valueOf(3000), 77L, NOW);
    when(userCouponRepository.findByUsedOrderIdWithLock(77L)).thenReturn(List.of(userCoupon));

    int released = userCouponService.releaseByOrder(77L);

    assertThat(released).isEqualTo(1);
    assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(userCoupon.getUsedOrderId()).isNull();
  }

  @Test
  void releaseByOrder는_복구할_쿠폰이_없으면_0을_반환한다() {
    userCouponService = service();
    when(userCouponRepository.findByUsedOrderIdWithLock(77L)).thenReturn(List.of());

    assertThat(userCouponService.releaseByOrder(77L)).isZero();
  }

  @Test
  void 복구된_쿠폰은_다시_할인계산에_사용할_수_있다() {
    userCouponService = service();
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    userCoupon.markUsed(BigDecimal.valueOf(5000), BigDecimal.valueOf(3000), 77L, NOW);
    when(userCouponRepository.findByUsedOrderIdWithLock(77L)).thenReturn(List.of(userCoupon));
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    userCouponService.releaseByOrder(77L);

    assertThat(userCouponService.calculateDiscount(OWNER_ID, 100L, BigDecimal.valueOf(5000)))
        .isEqualByComparingTo("3000");
  }

  @Test
  void 사용기간이_지난_쿠폰은_복구되어도_만료로_판단되어_사용할_수_없다() {
    userCouponService = service();
    Coupon expired = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.minusDays(3), NOW.minusDays(2), NOW.minusDays(2), NOW.minusDays(1)
    );
    UserCoupon userCoupon = UserCoupon.issue(mockUser(OWNER_ID), expired, NOW.minusDays(3));
    userCoupon.markUsed(BigDecimal.valueOf(5000), BigDecimal.valueOf(1000), 77L, NOW.minusDays(2));
    when(userCouponRepository.findByUsedOrderIdWithLock(77L)).thenReturn(List.of(userCoupon));
    when(userCouponRepository.findById(100L)).thenReturn(Optional.of(userCoupon));

    userCouponService.releaseByOrder(77L);

    assertThat(userCoupon.effectiveStatus(NOW)).isEqualTo(UserCouponStatus.EXPIRED);
    assertThat(expired.getUseEndAt()).isEqualTo(NOW.minusDays(1));
    assertThatThrownBy(() -> userCouponService.calculateDiscount(OWNER_ID, 100L, BigDecimal.valueOf(5000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_EXPIRED);
  }

  @Test
  void releaseByOrder는_주문ID가_없으면_거부된다() {
    userCouponService = service();

    assertThatThrownBy(() -> userCouponService.releaseByOrder(null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.INVALID_ORDER_ID);
  }

  // ---------- 내 쿠폰 목록 ----------

  private UserCouponSearchRequestDto searchCondition(int page, int size, UserCouponStatus status) {
    UserCouponSearchRequestDto condition = new UserCouponSearchRequestDto();
    condition.setPage(page);
    condition.setSize(size);
    condition.setStatus(status);
    return condition;
  }

  @Test
  void 내쿠폰_목록은_발급일시_최신순과_ID_내림차순으로_DB_페이지_조회한다() {
    userCouponService = service();
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    when(userCouponRepository.findMyCoupons(any(), pageableCaptor.capture()))
        .thenAnswer(inv -> new PageImpl<UserCoupon>(List.of(), inv.getArgument(1), 0));

    userCouponService.getMyCoupons(OWNER_ID, searchCondition(2, 30, null));

    Pageable pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageNumber()).isEqualTo(2);
    assertThat(pageable.getPageSize()).isEqualTo(30);
    assertThat(pageable.getSort())
        .containsExactly(Sort.Order.desc("issuedAt"), Sort.Order.desc("userCouponId"));
  }

  @Test
  void 상태필터에_따라_해당_조건의_조회쿼리를_사용한다() {
    userCouponService = service();
    when(userCouponRepository.findMyCouponsByStoredStatus(any(), any(), any()))
        .thenAnswer(inv -> new PageImpl<UserCoupon>(List.of(), inv.getArgument(2), 0));
    when(userCouponRepository.findMyUnusedCouponsNotExpired(any(), any(), any(), any()))
        .thenAnswer(inv -> new PageImpl<UserCoupon>(List.of(), inv.getArgument(3), 0));
    when(userCouponRepository.findMyUnusedCouponsExpired(any(), any(), any(), any()))
        .thenAnswer(inv -> new PageImpl<UserCoupon>(List.of(), inv.getArgument(3), 0));

    userCouponService.getMyCoupons(OWNER_ID, searchCondition(0, 20, UserCouponStatus.USED));
    userCouponService.getMyCoupons(OWNER_ID, searchCondition(0, 20, UserCouponStatus.AVAILABLE));
    userCouponService.getMyCoupons(OWNER_ID, searchCondition(0, 20, UserCouponStatus.EXPIRED));

    verify(userCouponRepository).findMyCouponsByStoredStatus(eq(OWNER_ID), eq(UserCouponStatus.USED), any());
    verify(userCouponRepository).findMyUnusedCouponsNotExpired(eq(OWNER_ID), eq(UserCouponStatus.USED), any(), any());
    verify(userCouponRepository).findMyUnusedCouponsExpired(eq(OWNER_ID), eq(UserCouponStatus.USED), any(), any());
    verify(userCouponRepository, never()).findMyCoupons(any(), any());
  }

  @Test
  void 내쿠폰_목록_응답은_쿠폰목록과_페이지정보를_분리하고_조회시점_상태를_담는다() {
    userCouponService = service();
    Coupon notYetUsable = coupon(
        DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        NOW.minusDays(2), NOW.plusDays(2), NOW.plusDays(1), NOW.plusDays(3)
    );
    UserCoupon usable = UserCoupon.issue(mockUser(OWNER_ID), usableNowCoupon(), NOW);
    UserCoupon notYet = UserCoupon.issue(mockUser(OWNER_ID), notYetUsable, NOW);
    when(userCouponRepository.findMyCoupons(eq(OWNER_ID), any()))
        .thenAnswer(inv -> new PageImpl<>(List.of(usable, notYet), inv.getArgument(1), 5));

    UserCouponPageResponseDto response = userCouponService.getMyCoupons(OWNER_ID, searchCondition(1, 3, null));

    assertThat(response.getCoupons()).hasSize(2);
    assertThat(response.getCoupons()).extracting(UserCouponResponseDto::getStatus)
        .containsExactly(UserCouponStatus.AVAILABLE, UserCouponStatus.AVAILABLE);
    assertThat(response.getCoupons()).extracting(UserCouponResponseDto::isUsableNow)
        .containsExactly(true, false);
    assertThat(response.getCoupons().get(0).getCoupon().getName()).isEqualTo("테스트쿠폰");
    UserCouponPageResponseDto.PageInfo pageInfo = response.getPageInfo();
    assertThat(pageInfo.getPage()).isEqualTo(1);
    assertThat(pageInfo.getSize()).isEqualTo(3);
    assertThat(pageInfo.getTotalElements()).isEqualTo(5);
    assertThat(pageInfo.getTotalPages()).isEqualTo(2);
    assertThat(pageInfo.isLast()).isTrue();
  }
}

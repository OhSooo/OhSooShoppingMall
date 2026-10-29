package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.CouponDiscountResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper.CouponMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.UserCouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.entity.User;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.exception.UserErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.identity.user.repository.UserRepository;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserCouponService {

  private static final String USER_COUPON_UNIQUE_CONSTRAINT = "uq_user_coupon";
  private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

  private final UserCouponRepository userCouponRepository;
  private final UserRepository userRepository;
  private final CouponService couponService;
  private final CouponMapper couponMapper;

  public UserCouponResponseDto issueCoupon(Long userId, Long couponId) {
    Coupon coupon = couponService.getCouponEntity(couponId);
    OffsetDateTime now = OffsetDateTime.now();

    if (!coupon.isIssuableAt(now)) {
      throw new BusinessException(CouponErrorCode.COUPON_ISSUE_PERIOD_INVALID);
    }
    if (userCouponRepository.existsByUser_UserIdAndCoupon_CouponId(userId, couponId)) {
      throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED);
    }

    User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    UserCoupon userCoupon = UserCoupon.issue(user, coupon, now);
    try {
      // 동시 요청이 exists 검사를 함께 통과한 경우 DB UNIQUE 제약에서 충돌한다.
      // IDENTITY 전략이라 트랜잭션 안의 save()(persist) 시점에 INSERT가 즉시 실행되어 여기서 예외를 잡을 수 있다.
      // ID 전략을 바꾸면 INSERT가 flush/커밋 시점으로 미뤄질 수 있으므로 이 처리도 함께 재검토해야 한다.
      userCouponRepository.save(userCoupon);
    } catch (DataIntegrityViolationException e) {
      if (isViolationOf(e, USER_COUPON_UNIQUE_CONSTRAINT)) {
        throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUED, e);
      }
      throw e;
    }

    return couponMapper.toUserCouponResponseDto(userCoupon, now);
  }

  // 원인 체인에서 PSQLException을 찾아 SQLState와 위반한 제약조건 이름을 비교한다.
  // Hibernate의 getConstraintName()은 영어 오류 메시지를 파싱하므로 DB 메시지가 한국어면 null이 된다.
  // 서버가 프로토콜 필드로 보내주는 제약조건 이름(ServerErrorMessage.getConstraint())은 메시지 언어와 무관하다.
  private boolean isViolationOf(DataIntegrityViolationException e, String constraintName) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof PSQLException psqlException) {
        ServerErrorMessage serverError = psqlException.getServerErrorMessage();
        return UNIQUE_VIOLATION_SQL_STATE.equals(psqlException.getSQLState())
            && serverError != null
            && constraintName.equalsIgnoreCase(serverError.getConstraint());
      }
    }
    return false;
  }

  @Transactional(readOnly = true)
  public List<UserCouponResponseDto> getMyCoupons(Long userId) {
    OffsetDateTime now = OffsetDateTime.now();
    return userCouponRepository.findByUser_UserIdOrderByIssuedAtDesc(userId).stream()
        .map(uc -> couponMapper.toUserCouponResponseDto(uc, now))
        .toList();
  }

  @Transactional(readOnly = true)
  public CouponDiscountResponseDto previewDiscount(Long userId, Long userCouponId, BigDecimal orderAmount) {
    UserCoupon userCoupon = getOwnedUserCoupon(userId, userCouponId);
    BigDecimal discountAmount = validateAndCalculate(userCoupon, orderAmount, OffsetDateTime.now());
    return couponMapper.toCouponDiscountResponseDto(userCoupon, orderAmount, discountAmount);
  }

  /**
   * 주문 금액 계산용 쿠폰 할인액 조회 (읽기 전용, 락/상태 변경 없음).
   * - orderAmount: 서버가 계산한 상품 금액 합계(배송비 제외, 원 단위)
   * - 미리보기와 동일한 검증/계산 규칙을 사용한다.
   * - 동시성 보장은 하지 않으므로, 실제 사용은 반드시 redeem()으로 다시 검증해야 한다.
   */
  @Transactional(readOnly = true)
  public BigDecimal calculateDiscount(Long userId, Long userCouponId, BigDecimal orderAmount) {
    UserCoupon userCoupon = getOwnedUserCoupon(userId, userCouponId);
    return validateAndCalculate(userCoupon, orderAmount, OffsetDateTime.now());
  }

  /**
   * 주문 생성 트랜잭션 안에서 쿠폰을 실제로 사용 처리한다. (AVAILABLE → USED)
   * - user_coupons 행에 비관적 락을 잡은 뒤 소유권/사용 가능 여부를 재검증하므로, 동시 요청 중 한 건만 성공한다.
   * - 재계산한 할인액이 calculateDiscount() 시점의 expectedDiscount와 다르면 COUPON_DISCOUNT_MISMATCH.
   * - MANDATORY: 호출자(주문 생성)의 트랜잭션에 반드시 참여해야 한다. 단독 커밋되면 주문 생성이 실패해도
   *   쿠폰이 USED로 남기 때문이다. 이 메서드에서 발생한 예외는 호출자가 삼키지 말고 그대로 전파해야 한다.
   *
   * @return 실제 적용된 할인액
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public BigDecimal redeem(
      Long userId, Long userCouponId, Long orderId, BigDecimal orderAmount, BigDecimal expectedDiscount
  ) {
    if (orderId == null) {
      throw new BusinessException(CouponErrorCode.INVALID_ORDER_ID);
    }
    UserCoupon userCoupon = getOwnedUserCouponWithLock(userId, userCouponId);
    OffsetDateTime now = OffsetDateTime.now();
    BigDecimal discountAmount = validateAndCalculate(userCoupon, orderAmount, now);
    if (expectedDiscount == null || discountAmount.compareTo(expectedDiscount) != 0) {
      throw new BusinessException(CouponErrorCode.COUPON_DISCOUNT_MISMATCH);
    }
    userCoupon.markUsed(orderAmount, discountAmount, orderId, now);
    return discountAmount;
  }

  /**
   * 주문에 사용된 쿠폰을 복구한다. (USED → AVAILABLE, 사용 이력 초기화)
   * - 미결제 주문 취소/만료, 결제 후 전액 환불, 부분 취소로 쿠폰 할인액이 전액 회수된 경우 호출한다.
   * - 사용 기간은 연장하지 않는다. 이미 기간이 지난 쿠폰은 복구되어도 EXPIRED로 판단된다.
   * - 멱등: 복구된 쿠폰은 used_order_id가 비워지므로, 같은 주문으로 다시 호출하면 대상이 없어 0을 반환한다.
   *   동시 호출은 행 락으로 직렬화되어 뒤의 요청이 대상 없음(0)이 된다.
   *
   * @return 복구된 쿠폰 수
   */
  public int releaseByOrder(Long orderId) {
    if (orderId == null) {
      throw new BusinessException(CouponErrorCode.INVALID_ORDER_ID);
    }
    List<UserCoupon> usedCoupons = userCouponRepository.findByUsedOrderIdWithLock(orderId);
    usedCoupons.forEach(UserCoupon::restore);
    return usedCoupons.size();
  }

  public CouponDiscountResponseDto useCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long orderId) {
    UserCoupon userCoupon = getOwnedUserCouponWithLock(userId, userCouponId);
    OffsetDateTime now = OffsetDateTime.now();
    BigDecimal discountAmount = validateAndCalculate(userCoupon, orderAmount, now);
    userCoupon.markUsed(orderAmount, discountAmount, orderId, now);

    return couponMapper.toCouponDiscountResponseDto(userCoupon, orderAmount, discountAmount);
  }

  private UserCoupon getOwnedUserCoupon(Long userId, Long userCouponId) {
    return verifyOwner(userCouponRepository.findById(userCouponId), userId);
  }

  private UserCoupon getOwnedUserCouponWithLock(Long userId, Long userCouponId) {
    return verifyOwner(userCouponRepository.findByIdWithLock(userCouponId), userId);
  }

  private UserCoupon verifyOwner(Optional<UserCoupon> found, Long userId) {
    UserCoupon userCoupon = found
        .orElseThrow(() -> new BusinessException(CouponErrorCode.USER_COUPON_NOT_FOUND));
    if (!userCoupon.isOwnedBy(userId)) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_ACCESS_DENIED);
    }
    return userCoupon;
  }

  // 미리보기/주문 계산/실제 사용이 공유하는 단일 검증·계산 경로
  private BigDecimal validateAndCalculate(UserCoupon userCoupon, BigDecimal orderAmount, OffsetDateTime now) {
    validateUsable(userCoupon, orderAmount, now);
    return userCoupon.getCoupon().calculateDiscount(orderAmount);
  }

  private void validateUsable(UserCoupon userCoupon, BigDecimal orderAmount, OffsetDateTime now) {
    if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0
        || !Coupon.isWholeWon(orderAmount)) {
      throw new BusinessException(CouponErrorCode.INVALID_ORDER_AMOUNT);
    }

    UserCouponStatus effectiveStatus = userCoupon.effectiveStatus(now);
    if (effectiveStatus == UserCouponStatus.USED) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_ALREADY_USED);
    }
    if (effectiveStatus == UserCouponStatus.EXPIRED) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_EXPIRED);
    }

    Coupon coupon = userCoupon.getCoupon();
    if (now.isBefore(coupon.getUseStartAt())) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_NOT_YET_USABLE);
    }
    if (!coupon.meetsMinOrderAmount(orderAmount)) {
      throw new BusinessException(CouponErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    }
  }
}

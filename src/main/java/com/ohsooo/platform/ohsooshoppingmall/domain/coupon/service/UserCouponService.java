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
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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
    validateUsable(userCoupon, orderAmount, OffsetDateTime.now());

    BigDecimal discountAmount = userCoupon.getCoupon().calculateDiscount(orderAmount);
    return couponMapper.toCouponDiscountResponseDto(userCoupon, orderAmount, discountAmount);
  }

  public CouponDiscountResponseDto useCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long orderId) {
    UserCoupon userCoupon = getOwnedUserCoupon(userId, userCouponId);
    validateUsable(userCoupon, orderAmount, OffsetDateTime.now());

    BigDecimal discountAmount = userCoupon.getCoupon().calculateDiscount(orderAmount);
    userCoupon.markUsed(orderAmount, discountAmount, orderId, OffsetDateTime.now());

    return couponMapper.toCouponDiscountResponseDto(userCoupon, orderAmount, discountAmount);
  }

  private UserCoupon getOwnedUserCoupon(Long userId, Long userCouponId) {
    UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
        .orElseThrow(() -> new BusinessException(CouponErrorCode.USER_COUPON_NOT_FOUND));
    if (!userCoupon.isOwnedBy(userId)) {
      throw new BusinessException(CouponErrorCode.USER_COUPON_ACCESS_DENIED);
    }
    return userCoupon;
  }

  private void validateUsable(UserCoupon userCoupon, BigDecimal orderAmount, OffsetDateTime now) {
    if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
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

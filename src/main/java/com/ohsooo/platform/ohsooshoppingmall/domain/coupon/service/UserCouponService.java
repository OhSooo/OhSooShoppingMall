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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserCouponService {

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
    userCouponRepository.save(userCoupon);

    return couponMapper.toUserCouponResponseDto(userCoupon, now);
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

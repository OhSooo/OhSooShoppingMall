package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

  boolean existsByUser_UserIdAndCoupon_CouponId(Long userId, Long couponId);

  List<UserCoupon> findByUser_UserIdOrderByIssuedAtDesc(Long userId);

  // 동일 쿠폰 동시 사용(redeem) 방지용 비관적 락 조회.
  // coupon을 fetch join하면 coupons 행까지 잠겨 같은 쿠폰 정책을 쓰는 다른 사용자끼리도 직렬화되므로 user_coupons 행만 잠근다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT uc FROM UserCoupon uc WHERE uc.userCouponId = :userCouponId")
  Optional<UserCoupon> findByIdWithLock(@Param("userCouponId") Long userCouponId);

  // 주문 취소/환불 시 복구 대상 조회. 동시 복구 요청과 복구-재사용 경합을 막기 위해 락을 건다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT uc FROM UserCoupon uc WHERE uc.usedOrderId = :orderId")
  List<UserCoupon> findByUsedOrderIdWithLock(@Param("orderId") Long orderId);
}

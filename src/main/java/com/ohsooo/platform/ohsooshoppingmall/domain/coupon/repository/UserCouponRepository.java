package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

  boolean existsByUser_UserIdAndCoupon_CouponId(Long userId, Long couponId);

  // ===== 내 쿠폰 목록(페이지) 조회 =====
  // coupon은 ManyToOne이라 fetch join해도 행이 늘지 않으므로 LIMIT/OFFSET이 DB에서 그대로 적용된다.
  // count 쿼리는 fetch join 없이 별도로 둔다. 정렬은 호출자가 Pageable의 Sort로 넘긴다.
  // AVAILABLE/EXPIRED 조건은 UserCoupon.effectiveStatus()와 같은 기준(사용 종료일)으로 DB에서 판단한다.

  @Query(
      value = "SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon "
          + "WHERE uc.user.userId = :userId",
      countQuery = "SELECT COUNT(uc) FROM UserCoupon uc "
          + "WHERE uc.user.userId = :userId"
  )
  Page<UserCoupon> findMyCoupons(@Param("userId") Long userId, Pageable pageable);

  @Query(
      value = "SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon "
          + "WHERE uc.user.userId = :userId AND uc.status = :status",
      countQuery = "SELECT COUNT(uc) FROM UserCoupon uc "
          + "WHERE uc.user.userId = :userId AND uc.status = :status"
  )
  Page<UserCoupon> findMyCouponsByStoredStatus(
      @Param("userId") Long userId, @Param("status") UserCouponStatus status, Pageable pageable);

  // 미사용 + 사용 종료일이 now 이후(포함) → effectiveStatus AVAILABLE (사용 시작 전 쿠폰 포함)
  @Query(
      value = "SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon c "
          + "WHERE uc.user.userId = :userId AND uc.status <> :usedStatus AND c.useEndAt >= :now",
      countQuery = "SELECT COUNT(uc) FROM UserCoupon uc JOIN uc.coupon c "
          + "WHERE uc.user.userId = :userId AND uc.status <> :usedStatus AND c.useEndAt >= :now"
  )
  Page<UserCoupon> findMyUnusedCouponsNotExpired(
      @Param("userId") Long userId, @Param("usedStatus") UserCouponStatus usedStatus,
      @Param("now") OffsetDateTime now, Pageable pageable);

  // 미사용 + 사용 종료일이 now 이전 → effectiveStatus EXPIRED
  @Query(
      value = "SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon c "
          + "WHERE uc.user.userId = :userId AND uc.status <> :usedStatus AND c.useEndAt < :now",
      countQuery = "SELECT COUNT(uc) FROM UserCoupon uc JOIN uc.coupon c "
          + "WHERE uc.user.userId = :userId AND uc.status <> :usedStatus AND c.useEndAt < :now"
  )
  Page<UserCoupon> findMyUnusedCouponsExpired(
      @Param("userId") Long userId, @Param("usedStatus") UserCouponStatus usedStatus,
      @Param("now") OffsetDateTime now, Pageable pageable);

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

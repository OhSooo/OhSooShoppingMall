package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

  boolean existsByUser_UserIdAndCoupon_CouponId(Long userId, Long couponId);

  List<UserCoupon> findByUser_UserIdOrderByIssuedAtDesc(Long userId);
}

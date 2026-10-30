package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

  List<Coupon> findByIssueStartAtLessThanEqualAndIssueEndAtGreaterThanEqual(
      OffsetDateTime now1, OffsetDateTime now2);
}

package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity;

/**
 * 사용자 발급 쿠폰의 상태.
 * - EXPIRED는 영속 컬럼에는 저장되지 않고, 조회/검증 시점에 쿠폰의 사용 종료일 기준으로 계산된다.
 *   (스케줄러 없이 lazy하게 판단하기 위함)
 */
public enum UserCouponStatus {
  AVAILABLE,
  USED,
  EXPIRED
}

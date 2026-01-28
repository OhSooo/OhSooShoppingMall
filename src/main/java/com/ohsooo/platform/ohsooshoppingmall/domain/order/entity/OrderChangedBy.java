package com.ohsooo.platform.ohsooshoppingmall.domain.order.entity;

/**
 * 주문상품 상태 변경 주체.
 * - OrderItemHistory에 "누가 바꿨는지" 기록할 때 사용한다.
 */
public enum OrderChangedBy {
  GENERAL, // 일반 사용자
  OWNER,   // 판매자(스토어 오너)
  ADMIN    // 관리자
}

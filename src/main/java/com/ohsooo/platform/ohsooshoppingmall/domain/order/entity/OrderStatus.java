package com.ohsooo.platform.ohsooshoppingmall.domain.order.entity;

/**
 * 주문 상태(주문 단위).
 * - 결제 전/후, 부분취소/부분환불, 완료 등의 흐름을 표현한다.
 */
public enum OrderStatus {
  CREATED,              // 주문 생성됨(결제 전)
  PAID,                 // 결제 완료
  CANCELED,             // 주문 전체 취소 (결제 전, 모든 아이템 취소 완료 시)
  PARTIALLY_CANCELED,   // 일부 취소 발생
  PARTIALLY_REFUNDED,   // 일부 환불 발생
  COMPLETED             // 구매(배송/정산 등) 전체 완료 처리
}

package com.ohsooo.platform.ohsooshoppingmall.domain.order.entity;

/**
 * 주문 상품 상태(주문 아이템 단위).
 * - 배송 및 취소/환불 플로우를 item 단위로 관리한다.
 */
public enum OrderItemStatus {
  ORDERED,            // 주문됨(결제 완료 후 또는 주문 확정 후)
  SHIPPED,            // 발송됨
  DELIVERED,          // 배송 완료
  CANCEL_REQUESTED,   // 취소 요청됨
  CANCELED,           // 취소 완료
  REFUND_REQUESTED,   // 환불 요청됨
  REFUNDED            // 환불 완료
}

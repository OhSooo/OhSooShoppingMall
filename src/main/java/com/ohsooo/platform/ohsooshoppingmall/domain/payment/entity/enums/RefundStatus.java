package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 환불 처리 상태를 나타내는 enum.
 *
 * Refund 엔티티에서 사용되며,
 * 환불 요청 → 성공 / 실패 흐름을 표현한다.
 */
public enum RefundStatus {
  REQUESTED,
  SUCCEEDED,
  FAILED
}

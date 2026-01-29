package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 결제 상태를 나타내는 enum.
 *
 * 결제는 상태 머신처럼 동작하며,
 * READY → (AUTHORIZED) → CAPTURED → (CANCELED / REFUNDED)
 * 또는 FAILED 흐름을 가진다.
 *
 * Payment 엔티티의 핵심 상태값.
 */
public enum PaymentStatus {
  READY,        // 결제 시도 시작 (결제창 진행 중 포함)
  AUTHORIZED,  // 승인 완료, 아직 매입 전 (선택적 사용)
  CAPTURED,    // 결제 확정 (돈이 실제로 빠져나감)
  FAILED,      // 결제 실패
  CANCELED,    // 결제 취소 (결제 성공 후 취소)
  REFUNDED     // 환불 완료
}

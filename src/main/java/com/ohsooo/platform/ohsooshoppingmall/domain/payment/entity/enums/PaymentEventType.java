package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 결제와 관련된 "이벤트 로그"의 종류를 나타내는 enum.
 *
 * - 결제 웹훅 수신
 * - 클라이언트 결제 확정(confirm) 요청
 * 같은 "사실 기록용 이벤트"를 구분하기 위해 사용된다.
 *
 * PaymentEvent 엔티티에서 사용됨.
 */
public enum PaymentEventType {
  WEBHOOK_APPROVED,
  WEBHOOK_FAILED,
  CLIENT_CONFIRM_REQUEST
}

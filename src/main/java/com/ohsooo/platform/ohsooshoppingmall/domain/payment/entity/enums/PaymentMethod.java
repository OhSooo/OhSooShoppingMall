package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 결제 수단을 나타내는 enum.
 *
 * - 카드 / 계좌이체 / 가상계좌 / 휴대폰 / 간편결제 등
 * - ZERO_AMOUNT: 쿠폰 할인 등으로 최종 결제 금액이 0원이라 실제 결제가 없는 경우 (PG 미경유, 서버 내부 전용)
 * 결제 생성 시 어떤 방식으로 결제를 시도했는지를 저장하기 위해 사용된다.
 *
 * Payment 엔티티에서 사용됨.
 */
public enum PaymentMethod {
  CARD,
  TRANSFER,
  VIRTUAL_ACCOUNT,
  MOBILE,
  EASY_PAY,
  ZERO_AMOUNT
}

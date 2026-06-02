package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 결제 수단을 나타내는 enum.
 *
 * - 카드 / 계좌이체 / 가상계좌 / 휴대폰 / 간편결제 등
 * 결제 생성 시 어떤 방식으로 결제를 시도했는지를 저장하기 위해 사용된다.
 *
 * Payment 엔티티에서 사용됨.
 */
public enum PaymentMethod {
  CARD,
  TRANSFER,
  VIRTUAL_ACCOUNT,
  MOBILE,
  EASY_PAY
}

package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 결제 대행사(PG)를 나타내는 enum.
 *
 * - 현재는 TOSS만 사용
 * - 추후 KAKAOPAY, NICE 등이 추가될 수 있음
 *
 * PgClientRouter에서 어떤 PG 클라이언트를 사용할지 결정할 때 사용된다.
 */
public enum PaymentProvider {
  TOSS,
  KAKAOPAY,
  NICE
}

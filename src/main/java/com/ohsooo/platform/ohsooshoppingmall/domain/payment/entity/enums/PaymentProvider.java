package com.ohsooo.platform.ohsooshoppingmall.domain.payment.entity.enums;

/**
 * 결제 대행사(PG)를 나타내는 enum.
 *
 * - 현재는 TOSS만 사용
 * - 추후 KAKAOPAY, NICE 등이 추가될 수 있음
 * - NONE: PG를 거치지 않은 결제 (0원 주문, 서버 내부 전용). PgClientRouter에서 라우팅 대상이 아님
 *
 * PgClientRouter에서 어떤 PG 클라이언트를 사용할지 결정할 때 사용된다.
 */
public enum PaymentProvider {
  TOSS,
  KAKAOPAY,
  NICE,
  NONE
}

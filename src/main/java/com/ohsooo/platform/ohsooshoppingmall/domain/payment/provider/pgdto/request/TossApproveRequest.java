package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스 결제 승인(확정) API 요청 Body DTO.
 *
 * 사용 시점:
 * - 클라이언트가 successUrl로 돌아온 뒤 서버가 confirm 요청을 받으면
 *   서버가 토스 "승인 API"를 호출할 때 이 DTO를 Body로 사용한다.
 *
 * 토스 승인 API는 보통 다음 3개가 필요:
 * - paymentKey: 토스가 발급한 결제 식별키
 * - orderId: 우리 주문 식별값(또는 결제 생성 시 토스에 넘긴 orderId)
 * - amount: 결제 금액(위변조 방지)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossApproveRequest {

  private String paymentKey;
  private String orderId;
  private Integer amount;
}

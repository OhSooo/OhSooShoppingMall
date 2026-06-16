package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스 결제 취소(환불) API 요청 Body DTO.
 *
 * 토스 취소 API: POST /v1/payments/{paymentKey}/cancel
 * - cancelAmount 미전송 시 전액 취소
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossCancelRequest {

  private String cancelReason;
  private Integer cancelAmount;
}

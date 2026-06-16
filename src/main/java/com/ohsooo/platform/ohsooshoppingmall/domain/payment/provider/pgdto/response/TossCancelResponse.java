package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스 결제 취소(환불) API 응답 DTO.
 *
 * 토스는 취소 성공 시 결제 전체 객체를 반환하므로
 * 필요한 필드만 수신한다.
 */
@Getter
@NoArgsConstructor
public class TossCancelResponse {

  private String paymentKey;
  private String status;
}

package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스 결제 승인(확정) API 응답 DTO.
 *
 * 사용 시점:
 * - TossPgClient가 토스 승인 API 호출 후 응답을 받을 때 역직렬화 대상
 * - 응답에서 필요한 값(결제 상태, approvedAt, transactionKey 등)을 추출해
 *   Payment 엔티티(pgPaymentKey/pgTransactionId/approvedAt/status 등) 갱신에 사용한다.
 *
 * 주의:
 * - 토스 응답 필드는 꽤 많다.
 * - 지금 단계에서는 "결제 상태 갱신에 필요한 최소 필드"만 먼저 선언한다.
 * - 추후 취소/부분환불/현금영수증 등 기능 늘리면 필드 추가하면 된다.
 */
@Getter
@NoArgsConstructor
public class TossApproveResponse {

  /**
   * 토스 결제키 (요청의 paymentKey와 동일한 값으로 오는 경우가 많음)
   */
  private String paymentKey;

  /**
   * 토스 거래 키(트랜잭션 식별값)
   * 우리 DB의 pg_transaction_id에 저장하기 적합
   */
  private String transactionKey;

  /**
   * 우리 시스템의 주문 ID (토스에 전달한 orderId)
   */
  private String orderId;

  /**
   * 승인된 총 결제금액
   */
  private Integer totalAmount;

  /**
   * 결제 수단 (CARD, EASY_PAY 등 - 토스 표기 문자열)
   */
  private String method;

  /**
   * 결제 상태 (DONE, CANCELED 등 - 토스 표기 문자열)
   */
  private String status;

  /**
   * 승인 시각 (ISO-8601 문자열로 오는 경우가 많음)
   * 엔티티 approvedAt(OffsetDateTime)로 변환해서 저장하는 걸 추천
   */
  private String approvedAt;
}

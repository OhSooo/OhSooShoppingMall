package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response;

/**
 * PG사별 승인 응답의 공통 인터페이스.
 * PaymentCommandService가 PG사 구현체에 의존하지 않고 승인 결과를 처리할 수 있게 한다.
 */
public interface PgApproveResponse {

  String getPaymentKey();

  String getTransactionKey();

  Integer getTotalAmount();

  String getApprovedAt();
}

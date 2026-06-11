package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossApproveRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossCancelRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.TossApproveResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.TossCancelResponse;

/**
 * PG사 API 호출을 추상화한 Client 인터페이스.
 *
 * 사용 시점:
 * - PaymentCommandService에서 "승인/취소/환불" 등 외부 PG 호출이 필요할 때 provider에 따라 선택 호출
 */
public interface PgClient {

  TossApproveResponse approve(TossApproveRequest request);

  TossCancelResponse cancel(String pgPaymentKey, TossCancelRequest request);
}

package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.client;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.config.PgProperties;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossApproveRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.request.TossCancelRequest;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.PgApproveResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.TossApproveResponse;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.pgdto.response.TossCancelResponse;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Toss Payments 전용 PG Client 구현체.
 *
 * 사용 시점:
 * - PaymentCommandService가 승인 API 호출이 필요할 때 PgClientRouter를 통해 선택되어 실행됨
 *
 * 구현 포인트:
 * - 승인(확정) API: POST /v1/payments/confirm
 * - 인증: Basic Auth (Base64(secretKey + ":"))
 */
@Component
@RequiredArgsConstructor
public class TossPgClient implements PgClient {

  private final RestClient tossRestClient;
  private final PgProperties pgProperties;

  @Override
  public PgApproveResponse approve(TossApproveRequest request) {
    try {
      return tossRestClient
          .post()
          .uri("/v1/payments/confirm")
          .contentType(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, buildBasicAuth(pgProperties.getToss().getSecretKey()))
          .body(request)
          .retrieve()
          .body(TossApproveResponse.class);

    } catch (RestClientResponseException e) {
      // 토스에서 내려준 에러 응답을 로그로 남기고 싶으면 여기서 e.getResponseBodyAsString() 활용
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    } catch (Exception e) {
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    }
  }

  @Override
  public TossCancelResponse cancel(String pgPaymentKey, TossCancelRequest request) {
    try {
      return tossRestClient
          .post()
          .uri("/v1/payments/{paymentKey}/cancel", pgPaymentKey)
          .contentType(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, buildBasicAuth(pgProperties.getToss().getSecretKey()))
          .body(request)
          .retrieve()
          .body(TossCancelResponse.class);

    } catch (RestClientResponseException e) {
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    } catch (Exception e) {
      throw new BusinessException(PaymentErrorCode.PG_API_CALL_FAILED, e);
    }
  }

  private String buildBasicAuth(String secretKey) {
    // Basic base64("test_sk_xxx:")
    String raw = secretKey + ":";
    String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }
}

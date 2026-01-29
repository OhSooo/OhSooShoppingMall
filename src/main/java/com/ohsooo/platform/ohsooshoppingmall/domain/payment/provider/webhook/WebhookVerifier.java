package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.webhook;

import jakarta.servlet.http.HttpServletRequest;

/**
 * PG 웹훅 서명 검증 인터페이스.
 *
 * 사용 시점:
 * - PaymentWebhookController에서 요청의 위변조 여부를 검증하고 싶을 때 호출
 *
 */
public interface WebhookVerifier {

  void verify(HttpServletRequest request, String rawBody);
}

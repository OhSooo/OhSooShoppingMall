package com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.webhook;

import com.ohsooo.platform.ohsooshoppingmall.domain.payment.exception.PaymentErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.payment.provider.config.PgProperties;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Toss 웹훅 서명 검증기.
 *
 * 사용 시점:
 * - 토스가 서버로 웹훅을 보낼 때, 헤더/바디 기반 서명 검증을 수행하기 위함
 *
 * 현재(MVP):
 * - 웹훅 미사용을 전제로 하므로 "구현 스텁" 형태.
 * - webhookSecret이 없거나 검증 로직이 확정되지 않은 상태에서 무조건 통과시키는 것은 보안상 위험하므로
 *   기본은 실패 처리(Forbidden)로 둔다.
 *
 * 추후:
 * - 토스 공식 스펙에 맞춰 "요청 헤더의 서명값 + rawBody + secret"으로 검증하도록 구현
 */
@Component
@RequiredArgsConstructor
public class TossWebhookVerifier implements WebhookVerifier {

  private final PgProperties pgProperties;

  @Override
  public void verify(HttpServletRequest request, String rawBody) {
    // 웹훅을 안 쓸 거면 컨트롤러에서 이 verify 자체를 호출하지 않으면 됨.

    String secret = pgProperties.getToss().getWebhookSecret();
    if (secret == null || secret.isBlank()) {
      // 웹훅 시크릿이 없는데 검증을 통과시키면 누구나 임의의 웹훅을 보낼 수 있어서 위험함
      throw new BusinessException(PaymentErrorCode.PG_SIGNATURE_INVALID);
    }

    // TODO: 토스 웹훅 서명 검증 스펙 확정 시 구현
    // 1) 요청 헤더에서 signature 관련 값 추출
    // 2) rawBody + secret으로 HMAC 등 계산
    // 3) 헤더 서명과 비교
    throw new BusinessException(PaymentErrorCode.PG_SIGNATURE_INVALID);
  }
}
